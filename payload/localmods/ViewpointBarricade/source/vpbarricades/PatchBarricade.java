package vpbarricades;
import me.zed_0xff.zombie_buddy.Patch;
@Patch(className="viewpoint.world.PackGather",methodName="object")
public class PatchBarricade {
    @Patch.OnEnter(skipOn=true) public static boolean enter(@Patch.AllArguments Object[] args,@Patch.Local("barricadeHandled") boolean handled){
        handled=Barricades.capture(args[0],(float)args[2],(float)args[3],(float)args[4]);return handled;
    }
    @Patch.OnExit public static void exit(@Patch.Local("barricadeHandled") boolean handled,@Patch.Return(readOnly=false) boolean result){
        if(handled)result=true;
    }
}
