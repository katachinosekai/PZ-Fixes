package vpbarricades;
import java.lang.reflect.*;
import java.util.*;

public final class Barricades {
    private static Api api;
    private static boolean failed;
    private static final ThreadLocal<Set<Object>> seen=ThreadLocal.withInitial(()->Collections.newSetFromMap(new IdentityHashMap<>()));
    private static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    private static Method method(Class<?> c,String n,Class<?>...p)throws Exception{Method m=c.getDeclaredMethod(n,p);m.setAccessible(true);return m;}
    private static final class Api {
        final Class<?> barricade;
        final Method spriteOf,spriteName,opening,square,north,planks,metal,bars,health,direction,texture,ready,id,named,batch,emit,rise,ownerOf;
        final Field manager,x,y,z,owner,lift,squareIndex,pending;
        final Object N,northMesh,westMesh;
        Api()throws Exception{
            barricade=Class.forName("zombie.iso.objects.IsoBarricade");
            Class<?> object=Class.forName("zombie.iso.IsoObject"),sq=Class.forName("zombie.iso.IsoGridSquare"),
                able=Class.forName("zombie.iso.objects.interfaces.BarricadeAble"),sp=Class.forName("zombie.iso.sprite.IsoSprite"),
                sm=Class.forName("zombie.iso.sprite.IsoSpriteManager"),dir=Class.forName("zombie.iso.IsoDirections"),
                tx=Class.forName("zombie.core.textures.Texture"),wm=Class.forName("viewpoint.world.WorldMesher"),
                mesh=Class.forName("viewpoint.world.TileMesh"),b=Class.forName("viewpoint.world.Recipe$Batch");
            spriteOf=object.getMethod("getSprite");spriteName=sp.getMethod("getName");
            opening=barricade.getMethod("getBarricadedObject");square=object.getMethod("getSquare");north=able.getMethod("getNorth");
            planks=barricade.getMethod("getNumPlanks");metal=barricade.getMethod("isMetal");bars=barricade.getMethod("isMetalBar");health=barricade.getMethod("getHealth");
            direction=object.getMethod("getForwardIsoDirection");manager=field(sm,"instance");named=sm.getMethod("getNamedMap");N=dir.getField("N").get(null);
            texture=sp.getMethod("getTextureForCurrentFrame",dir);ready=tx.getMethod("isReady");id=tx.getMethod("getTextureId");
            x=field(sq,"x");y=field(sq,"y");z=field(sq,"z");
            batch=method(wm,"batch",id.getReturnType());emit=method(wm,"emit",b,mesh,tx,float.class,float.class,float.class,int.class);
            rise=wm.getMethod("rise",object);owner=field(wm,"owner");lift=field(wm,"lift");squareIndex=field(wm,"square");pending=field(wm,"pending");
            ownerOf=method(Class.forName("viewpoint.visibility.Owner"),"of",int.class,int.class);
            Constructor<?> constructor=mesh.getDeclaredConstructor(float[].class);constructor.setAccessible(true);
            northMesh=constructor.newInstance((Object)BarricadeMesh.build(true));westMesh=constructor.newInstance((Object)BarricadeMesh.build(false));
        }
    }
    public static synchronized void validate(){
        if(api!=null||failed)return;
        try{api=new Api();System.out.println("[ViewpointBarricades] 1.0.11 ready: fitted barricade artwork and blank stack layers supported; debug probes disabled.");}
        catch(Exception e){failed=true;System.err.println("[ViewpointBarricades] Initialization failed; original rendering retained: "+e);e.printStackTrace();}
    }
    public static void reset(){seen.get().clear();}
    public static boolean capture(Object object,float x,float y,float z){
        if(failed||object==null)return false;if(api==null)validate();if(failed||!api.barricade.isInstance(object))return false;
        try{
            if(seen.get().contains(object))return true;
            Object currentSprite=api.spriteOf.invoke(object);
            String currentName=currentSprite==null?null:(String)api.spriteName.invoke(currentSprite);
            String fitted=FittedArt.canonical(currentName);
            if(FittedArt.blank(currentName)){seen.get().add(object);return true;}
            Object opening=api.opening.invoke(object),sq=api.square.invoke(object);
            if(opening==null||sq==null)return false; // Orphaned map objects need their authored fallback.
            Object ownerSquare=api.square.invoke(opening);if(ownerSquare==null)return false;
            boolean north=(boolean)api.north.invoke(opening);
            int ox=api.x.getInt(ownerSquare),oy=api.y.getInt(ownerSquare),sx=api.x.getInt(sq),sy=api.y.getInt(sq);
            boolean opposite=ox!=sx||oy!=sy;
            if(api.z.getInt(ownerSquare)!=api.z.getInt(sq)|| (opposite&&(north?(ox!=sx||oy!=sy+1):(oy!=sy||ox!=sx+1))))return false;
            int count=(int)api.planks.invoke(object);boolean metal=(boolean)api.metal.invoke(object),bars=(boolean)api.bars.invoke(object);
            String tile=fitted!=null?fitted:metal?"constructedobjects_01_"+((int)api.health.invoke(object)<=2500?27:25)
                :bars?"constructedobjects_01_53":count>=1&&count<=4?"carpentry_01_"+(9+2*(count-1)):null;
            if(tile==null){seen.get().add(object);return true;}
            Object sprite=((Map<?,?>)api.named.invoke(api.manager.get(null))).get(tile);if(sprite==null)return false;
            Object texture=api.texture.invoke(sprite,api.N);if(texture==null)return false;
            if(!(boolean)api.ready.invoke(texture)||api.id.invoke(texture)==null){((boolean[])api.pending.get(null))[0]=true;return true;}
            float[] anchor=BarricadeMesh.anchor(north,opposite,x,y,ox,oy,sx,sy);
            // Shift N/S wooden planks north by 1/32 tile on both sides.
            // East/west and untested metal retain the 1.0.0 placement.
            if(north&&!metal&&!bars)anchor[1]-=.03125f;
            // Same-tile side lies south of an N/S opening or east of an E/W opening.
            // Move wooden sprite planks toward the wall; preserve opposite-side and metal placement.
            if(!opposite&&!metal&&!bars){if(north)anchor[1]-=.0625f;else anchor[0]-=.0625f;}
            // Fitted metal artwork needs a world-north correction of 1/16 tile.
            if(fitted!=null&&(metal||bars))anchor[1]-=.0625f;
            float height=z-((Number)api.rise.invoke(null,object)).floatValue();
            // Always use aligned vanilla sprite-art geometry, including wooden planks.
            Object batch=api.batch.invoke(null,api.id.invoke(texture));
            int oldOwner=api.owner.getInt(null),oldLift=api.lift.getInt(null);
            try{
                // PackGather is called before WorldMesher sets these for the object.
                // Opposite-side barricades live in the adjacent tile, not on that
                // tile's N/W edge. Keep object ownership instead of a false wall edge.
                api.owner.setInt(null,(int)api.ownerOf.invoke(null,api.squareIndex.getInt(null),0));api.lift.setInt(null,0);
                api.emit.invoke(null,batch,north?api.northMesh:api.westMesh,texture,anchor[0],anchor[1],height,0);
            }finally{api.owner.setInt(null,oldOwner);api.lift.setInt(null,oldLift);}
            seen.get().add(object);
            return true;
        }catch(Exception e){failed=true;System.err.println("[ViewpointBarricades] Correction disabled; original rendering retained: "+e);e.printStackTrace();return false;}
    }
}
