package bitcoinminner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlockChainConnection {
    public Header GenerateHeader(){
        return new Header(getUnconfirmedTXs(), GetPreviousHash(), GetVersion(), GetBits(), GetTime());  
    }
    
    private long GetBits(){
        return 0x19015f53;
    }
    
    private long GetVersion(){
        return 2;
    }
    
    private long GetTime(){
        //TODO: update this to return the actual current time
        return 0x53058b35;
    }
    
    private String GetPreviousHash(){
        BufferedReader in = null;
        String hash = "";
        try {
            in = MakeWebRequest("https://blockchain.info/latestblock");
            String inputLine;
            while ((inputLine = in.readLine()) != null){
                if(inputLine.contains("\"hash\":")){
                    hash = inputLine.substring(12, 76);
                    break;
                }
            }
        } catch (Exception e) {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(BlockChainConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return hash;
    }
    
    private BufferedReader MakeWebRequest(String url) throws Exception{
        URL Request = new URL(url);
        URLConnection bc = Request.openConnection();
        BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                    bc.getInputStream()));
        return in;
    } 
    //This will only pull 10 unconfirmed tx due to API throttling
    private String[] getUnconfirmedTXs(){
        int index = 0;
        String[] ret = new String[10];
        BufferedReader in = null;
        try {
        in = MakeWebRequest("https://blockchain.info/unconfirmed-transactions?format=json");
            String inputLine;
            
            while ((inputLine = in.readLine()) != null){
                if(inputLine.contains("\"hash\":")){
                    ret[index] = inputLine.substring(11, 75);
                    index++;
                }
            }      
        } catch (Exception ex){
            Logger.getLogger(BlockChainConnection.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
                Logger.getLogger(BlockChainConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return ret;
    }
}
