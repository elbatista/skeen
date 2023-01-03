import skeen.SkeenClient;
import skeen.TpccSkeenClient;
import util.ArgsParser;

public class MainClient {
    public static void main(String args[]){
        ArgsParser p = ArgsParser.getClientParser(args);
        if(p.isTpcc()){
            new TpccSkeenClient(p.getId(), p);
        }
        else{
            System.out.println(">>>>>");
            new SkeenClient(p.getId(), p, true);
        }
    }
}