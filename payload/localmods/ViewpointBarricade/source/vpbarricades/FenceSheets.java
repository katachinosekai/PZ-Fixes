package vpbarricades;
import java.lang.reflect.*;
import java.util.Map;
/** Vanilla sheet support; all compatibility metadata is resolved once. */
public final class FenceSheets {
 private static Api a; private static boolean failed;
 private static Method method(Class<?> c,String n,Class<?>...p)throws Exception{Method m=c.getDeclaredMethod(n,p);m.setAccessible(true);return m;}
 private static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 private static class Api {
  Class<?> curtain; Method sprite,name,texture,id,ready,named,batch,emit,ownerOf,rise;
  Field manager,owner,lift,square,pending;
  Object N,northMesh,westMesh;
  Api()throws Exception{
   Class<?> obj=Class.forName("zombie.iso.IsoObject"),sp=Class.forName("zombie.iso.sprite.IsoSprite"),dir=Class.forName("zombie.iso.IsoDirections"),tx=Class.forName("zombie.core.textures.Texture"),wm=Class.forName("viewpoint.world.WorldMesher"),mesh=Class.forName("viewpoint.world.TileMesh"),sm=Class.forName("zombie.iso.sprite.IsoSpriteManager");
   curtain=Class.forName("zombie.iso.objects.IsoCurtain");
   sprite=obj.getMethod("getSprite");name=sp.getMethod("getName");texture=sp.getMethod("getTextureForCurrentFrame",dir);id=tx.getMethod("getTextureId");ready=tx.getMethod("isReady");N=dir.getField("N").get(null);
   manager=field(sm,"instance");named=sm.getMethod("getNamedMap");batch=method(wm,"batch",id.getReturnType());emit=method(wm,"emit",batch.getReturnType(),mesh,tx,float.class,float.class,float.class,int.class);rise=wm.getMethod("rise",obj);
   owner=field(wm,"owner");lift=field(wm,"lift");square=field(wm,"square");pending=field(wm,"pending");ownerOf=method(Class.forName("viewpoint.visibility.Owner"),"of",int.class,int.class);
   Constructor<?> ctor=mesh.getDeclaredConstructor(float[].class);ctor.setAccessible(true);northMesh=ctor.newInstance((Object)BarricadeMesh.build(true));westMesh=ctor.newInstance((Object)BarricadeMesh.build(false));

  }
 }
 public static synchronized void validate(){if(a!=null||failed)return;try{a=new Api();System.out.println("[ViewpointFenceSheets] Barricade Fix 1.0.11: fence sheets and tarps; debug probes disabled.");}catch(Exception e){fail(e);}}
 private static void fail(Exception e){failed=true;System.err.println("[ViewpointFenceSheets] Fence sheet support disabled: "+e);}
 public static int tile(String name){
  if(name==null||!name.matches("fencesheets_0[1-6]_[0-9]+"))return -1;
  try{return Integer.parseInt(name.substring(name.lastIndexOf('_')+1));}catch(NumberFormatException e){return -1;}
 }
 public static String art(String name,int n){return name.substring(0,name.lastIndexOf('_')+1)+(n-n%4+2);}
 public static boolean window(Object object,float px,float py,float pz){
  if(object==null||failed)return false;if(a==null)validate();if(failed||!a.curtain.isInstance(object))return false;
  try{
   Object sp=a.sprite.invoke(object);String spriteName=sp==null?null:(String)a.name.invoke(sp);int n=tile(spriteName);if(n<0)return false;
   int direction=n%4;boolean north=direction>=2;
   Object art=((Map<?,?>)a.named.invoke(a.manager.get(null))).get(art(spriteName,n));if(art==null)return false;
   Object tex=a.texture.invoke(art,a.N);if(tex==null)return false;
   if(!(boolean)a.ready.invoke(tex)||a.id.invoke(tex)==null){((boolean[])a.pending.get(null))[0]=true;return true;}
   float offset=(direction==0||direction==2)?-.43f:.45f;
   if(north)py+=offset;else px+=offset;
   Object batch=a.batch.invoke(null,a.id.invoke(tex));int owner=a.owner.getInt(null),lift=a.lift.getInt(null);
   try{a.owner.setInt(null,(int)a.ownerOf.invoke(null,a.square.getInt(null),0));a.lift.setInt(null,0);a.emit.invoke(null,batch,north?a.northMesh:a.westMesh,tex,px,py,pz-((Number)a.rise.invoke(null,object)).floatValue(),0);}
   finally{a.owner.setInt(null,owner);a.lift.setInt(null,lift);}return true;
  }catch(Exception e){fail(e);return false;}
 }
}
