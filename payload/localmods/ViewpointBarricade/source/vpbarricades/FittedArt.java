package vpbarricades;
/** Fitted Barricades variants contain W/E/N/S artwork in groups of four. */
public final class FittedArt {
 public static boolean blank(String name){return name!=null && (name.equals("fittedbarricades_01_0")||name.equals("fittedbarricades_01_1")||name.equals("fittedbarricades_01_2")||name.equals("fittedbarricades_01_3"));}
 public static String canonical(String name){
  if(name==null||!name.startsWith("fittedbarricades_"))return null;
  int split=name.lastIndexOf('_');
  if(split<="fittedbarricades_".length())return null;
  try {int index=Integer.parseInt(name.substring(split+1));if(index<0)return null;
   return name.substring(0,split+1)+(index-index%4+2);
  }catch(NumberFormatException e){return null;}
 }
}
