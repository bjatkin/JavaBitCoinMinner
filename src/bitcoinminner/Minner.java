package bitcoinminner;

import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;

public class Minner {

    private static long ver = 2;
    private static String prev_block = "000000000000000117c80378b8da0e33559b5997f2ad55e2f7d18ec1975b9717";
    private static String mrkl_root =  "871714dcbae6c8193a2bb9b2a69fe1c0440399f38d94b3a0f1b447275a29978a";
    private static long time_ = 0x53058b35;
    private static long bits = 0x19015f53;
    
    public static void main(String[] args) {
        //Pack all the bald header data
        //Pack the longs
        byte[] bVer = ToByteArray(ver);
        byte[] bTime = ToByteArray(time_);
        byte[] bBits = ToByteArray(bits);
        
        //Pack the hashes
        byte[] bPrev = ToByteArray(prev_block);
        byte[] bMrkl = ToByteArray(mrkl_root);
        
        //Create the bald Header
        byte[] Header = CombineByteArrays(80, bVer, ReverseByteArray(bPrev), ReverseByteArray(bMrkl), bTime, bBits);
        
        //Calculate the difficulty
        byte[] Difficulty = CalculateDifficulty((int)bits);
        
        System.out.println("Current difficulty target:");
        PrintByteArray(ReverseByteArray(Difficulty));
        
        //Set up our variables for working thorugh the block
        byte[] hash = new byte[32];
        boolean success = false;
        long nonce = 0;
        
        System.out.println("\nStarting block");
        //Work through the current nonce
        while (nonce < 0x100000000l) {
            //add the nonce to the byte array
            Header[76] = (byte) (nonce & 0xFF);
            Header[77] = (byte) ((nonce >> 8) & 0xFF);
            Header[78] = (byte) ((nonce >> 16) & 0xFF);
            Header[79] = (byte) ((nonce >> 24) & 0xFF);
            
            //Compute the DoulbeShaw256
            hash = DoubleSHA256(Header);
            
            //Check to see if the hash is small enough
            for(int c = hash.length - 1; c >=0; c--){
                //we have to mask these values as java automatically casts them to integers
                if ((hash[c] & 0x000000FF) > (Difficulty[c] & 0x000000FF)){
                    break;
                }
                if ((hash[c] & 0x000000FF) < (Difficulty[c] & 0x000000FF)){
                    success = true;
                    break;
                }
            }
            
            if (success) {break;}
            
            //Print our progress so we have some indication of where we are.
            if (nonce % 0x7FFFFFl == 0){
                System.out.println("Nonce: " + Long.toHexString(nonce));
            }
            nonce++;
        }
        
        if (success) {
            System.out.println("\nSUCCESS!");
            System.out.println("Full Header:");
            PrintByteArray(Header);
            System.out.println("Hash:");
            PrintByteArray(ReverseByteArray(hash));
            System.out.println("Nonce:");
            System.out.println(nonce); 
        } else {
            System.out.println("Failed to find an appropriate hash");
        }
    }
    
    public static void PrintByteArray(byte[] b){
        StringBuffer hexString = new StringBuffer();
        
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(0xff & b[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        
        System.out.println(hexString.toString());
    }
    
    public static byte[] CalculateDifficulty(int bits){
        //https://en.bitcoin.it/wiki/Difficulty
        byte one = (byte) (bits & 0xFF);
        byte two = (byte) ((bits >> 8) & 0xFF);
        byte three = (byte) ((bits >> 16) & 0xFF);
        int exp = bits >> 24;
        
        byte[] dif = new byte[32];
        
        for(int c = 0; c < dif.length; c++){
            //
            //Uncomment this section for a non-truncated target
            //
            /*if (c < exp - 3){
                dif[c] = (byte) 0xFF;
                continue;
            }*/
            if (c == exp - 1){
                dif[c] = three;
                continue;
            }
            if (c == exp - 2){
                dif[c] = two;
                continue;
            }
            if (c == exp - 3){
                dif[c] = one;
                continue;
            }
            dif[c] = (byte) 0x00;
        }
        
        return dif;
    }
    
    public static byte[] DoubleSHA256(byte[] base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(digest.digest(base));
            return hash;
        
        } catch(Exception ex){
           throw new RuntimeException(ex);
        }
    }
    
    public static byte[] ReverseByteArray(byte[] array){
        byte[] ret = new byte[array.length];
        for(int c = 0; c < array.length; c++){
            ret[array.length - c - 1] = array[c];
        }
        return ret;
    }
    
    public static byte[] CombineByteArrays(int FinalLen, byte[]... arrays){
        byte[] ret = new byte[FinalLen];
        int count = 0;
        for (byte[] b: arrays){
            for (byte byt: b){
                ret[count] = byt;
                count++;
            }
        }
        return ret;
    }
  
    public static byte[] ToByteArray(String hash){
        if (hash.length() % 16 != 0){
            return null;
        }
        byte[] ret = new byte[hash.length()/2];
        byte[] b;
        long l;
        
        for(int c = 0; c < hash.length()/16; c++){
            String HexByte = hash.substring(16*c, (16*c)+16);
            l = Long.parseUnsignedLong(HexByte, 16);
            ret[8*c+7] = (byte) (l & 0xFF);
            ret[8*c+6] = (byte) ((l >> 8) & 0xFF);
            ret[8*c+5] = (byte) ((l >> 16) & 0xFF);
            ret[8*c+4] = (byte) ((l >> 24) & 0xFF);
            ret[8*c+3] = (byte) ((l >> 32) & 0xFF);
            ret[8*c+2] = (byte) ((l >> 40) & 0xFF);
            ret[8*c+1] = (byte) ((l >> 48) & 0xFF);
            ret[8*c] = (byte) ((l >> 56) & 0xFF);
        }
        
        return ret;
    }
    
    public static byte[] ToByteArray(long a){
        return new byte[] {
            (byte) (a & 0xFF),
            (byte) ((a >> 8) & 0xFF),
            (byte) ((a >> 16) & 0xFF),
            (byte) ((a >> 24) & 0xFF)   
        };
    }
}
