import skeen.SkeenClientNoLocality;
import util.ArgsParser;

public class MainClient {
    public static void main(String args[]){
        ArgsParser p = ArgsParser.getClientParser(args);
        
        new SkeenClientNoLocality(p.getId(), p, true);
    }
}