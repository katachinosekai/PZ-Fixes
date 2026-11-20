package vpbarricades;
import java.util.ArrayList;

/** A thin, two-sided textured volume. Alpha in the game's directional art retains
 * plank gaps, nails, damaged sheets and metal bars without model-pack assets. */
public final class BarricadeMesh {
    private static final float HALF_WIDTH=.55f, HEIGHT=2.4494896f, HALF_DEPTH=.015f;
    public static float[] build(boolean north){
        float a=-HALF_WIDTH,b=HALF_WIDTH,c=0,d=HEIGHT,e=-HALF_DEPTH,f=HALF_DEPTH;
        ArrayList<Float> out=new ArrayList<>();
        quad(out,north,new float[][]{{a,c,f},{b,c,f},{b,d,f},{a,d,f}},0,0,1);
        quad(out,north,new float[][]{{b,c,e},{a,c,e},{a,d,e},{b,d,e}},0,0,-1);
        quad(out,north,new float[][]{{a,c,e},{a,c,f},{a,d,f},{a,d,e}},-1,0,0);
        quad(out,north,new float[][]{{b,c,f},{b,c,e},{b,d,e},{b,d,f}},1,0,0);
        quad(out,north,new float[][]{{a,d,f},{b,d,f},{b,d,e},{a,d,e}},0,1,0);
        quad(out,north,new float[][]{{a,c,e},{b,c,e},{b,c,f},{a,c,f}},0,-1,0);
        float[] result=new float[out.size()];for(int i=0;i<result.length;i++)result[i]=out.get(i);return result;
    }
    private static void quad(ArrayList<Float> out,boolean north,float[][] p,float nx,float ny,float nz){
        for(int i:new int[]{0,1,2,0,2,3}){
            float x=p[i][0],y=p[i][1],z=p[i][2];
            // UVs are native 1x sprite-frame pixels. Reproject onto the canonical
            // north-facing barricade plane, independent of the corrected position.
            float u=32f*(x+.4061f+1f),v=112f+16f*(x-.4061f)-39.191833f*y;
            // Rotate +90 degrees about up for west/east: preserve winding/normals.
            float[] vertex={north?x:z,y,north?z:-x,u,v,north?nx:nz,ny,north?nz:-nx};
            for(float value:vertex)out.add(value);
        }
    }
    public static float[] anchor(boolean north,boolean opposite,float captureX,float captureY,
                                 int ownerX,int ownerY,int squareX,int squareY){
        float x=captureX+ownerX-squareX,y=captureY+ownerY-squareY;
        // Match Viewpoint's native wall planes relative to the owning tile center.
        float offset=(north?-.4375f:-.46875f)+(opposite?-.065f:.065f);
        if(north)y+=offset;else x+=offset;
        return new float[]{x,y};
    }
}
