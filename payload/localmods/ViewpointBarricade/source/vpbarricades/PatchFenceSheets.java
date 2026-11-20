package vpbarricades;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="viewpoint.world.WorldMesher",methodName="drawnElsewhere")
public class PatchFenceSheets {
 @Patch.OnEnter(skipOn=true) public static boolean enter(@Patch.Argument(1) Object object,@Patch.Argument(3) float x,@Patch.Argument(4) float y,@Patch.Argument(5) float z,@Patch.Local("fenceSheetHandled") boolean handled){handled=FenceSheets.window(object,x,y,z);return handled;}
 @Patch.OnExit public static void exit(@Patch.Local("fenceSheetHandled") boolean handled,@Patch.Return(readOnly=false) boolean result){if(handled)result=true;}
}
