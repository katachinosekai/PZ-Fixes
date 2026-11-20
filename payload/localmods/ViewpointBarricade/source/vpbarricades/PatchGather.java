package vpbarricades;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="viewpoint.world.WorldMesher",methodName="gather")
public class PatchGather {
    @Patch.OnEnter public static void enter(){Barricades.reset();}
    @Patch.OnExit public static void exit(){Barricades.reset();}
}
