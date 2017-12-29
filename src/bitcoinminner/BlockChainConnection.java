package bitcoinminner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlockChainConnection {
    public static Header GenerateHeader(){
        return new Header(getUnconfirmedTXs(), GetPreviousHash(), GetVersion(), GetBits(), GetTime());  
    }
    
    private static long GetBits(){
        return 402759343;
    }
    
    private static long GetVersion(){
        return 4;
    }
    
    private static long GetTime(){
        //TODO: update this to return the actual current time
        return 0x53058b35;
    }
    
    private static String GetPreviousHash(){
        //Test data
        //return "00000000000000000c96e86d6fa0315cac2a623a72c84953b2d811cb05ebaa35";
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
    
    private static BufferedReader MakeWebRequest(String url) throws Exception{
        URL Request = new URL(url);
        URLConnection bc = Request.openConnection();
        BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                    bc.getInputStream()));
        return in;
    } 
    //This will only pull 10 unconfirmed tx due to API throttling
    private static String[] getUnconfirmedTXs(){
      //Test Data
      /*return new String[]{"d4141b89d42f969879f4cede1e8186ad110cf88ca862c6cbd7020acd9df6ce47",
                          "7220a57c6a1739a2d1bfbb0f05556d6d36c4ac10b966e3f35074faf3fafda246",
                          "8f47e13379f9a8c816f8e8aac3f7e898199335294d18c7bcb2c00404945f4eab",
                          "83af78a02b8777fe1ac98ecea9bca5c3b4897dff394acf78d78758d67d110403",
                          "61cb55c257b018d5224659a0bf4eee53ef2f82fd698be403115257ff4306491f",
                          "c9d4a8e7a761ab530818c4f9e1eb82a7b7acb1c780af638c607bcf85d2194e30",
                          "17f854fd625cef7df6bcf592f531ae57bd208aeba9135f9c6c3258bdaf970180",
                          "0841ce8081a0d605f68253c575ff1de5b1371b555ec84dacd7a2c98ea8b0131b",
                          "8f737a4be55d8dde4b72bcaff9b88561db4be08baddb5d3ad47be576b5d98d29",
                          "3bd01c4f8c4af006829623ee2c4aa67f64137e6552fb709e75b43da762d443f9",
                          "ba4ac0415b559f8f5f8918a815120bf68dd1e1c0e37587e86ce1fe6dfd1390fd"};*/
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
