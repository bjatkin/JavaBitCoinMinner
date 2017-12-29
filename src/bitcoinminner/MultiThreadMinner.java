package bitcoinminner;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Random;
import org.apache.commons.codec.digest.DigestUtils;
/**
 *
 * @author brandon
 */
public class MultiThreadMinner {
    
    private static final int NUM_THREADS=Runtime.getRuntime().availableProcessors();
    
    public static void main(String[] args) throws InterruptedException {
        long splitNonce = 0x100000000l/NUM_THREADS;
       
        //Get a new block header object.
        Header BlockHeader = BlockChainConnection.GenerateHeader();
        byte[] Header = BlockHeader.getHeader();
        
        //Calculate the difficulty
        byte[] Difficulty = BlockHeader.getDifficulty();

        //Create a syncronizer class to stop all threads upon success
        Syncronizer s = new Syncronizer();
        
        //Start all of our miners
        ThreadedMinner[] minners = new ThreadedMinner[NUM_THREADS];
        for(int c = 0; c < NUM_THREADS; c++){
            minners[c] = new ThreadedMinner(Header, splitNonce*c, (splitNonce*c+splitNonce), Difficulty, c, s);
            new Thread(minners[c]).start();
            System.out.println("Searching with Thread " +  (c+1));
        }
    }
    
    public static void WriteFile(String fp, String data){
        BufferedWriter bw = null;
	FileWriter fw = null;

            try {
                fw = new FileWriter(fp);
                bw = new BufferedWriter(fw);
                bw.write(data);
                    
                System.out.println(fp + " Done!");

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bw != null)
                        bw.close();
                    
                    if (fw != null)
                        fw.close();
		} catch (IOException ex) {
                    ex.printStackTrace();
		}
            }
    }
}

class Syncronizer {
    private boolean StopThread;
    Syncronizer(){
        StopThread = false;
    }
    public void Stop(){
        StopThread = true;
    }
    public boolean isStopped(){
        return StopThread;
    }
}

class ThreadedMinner implements Runnable {
    private final byte[] HEADER;
    private final long   BEGINNING_NONCE;
    private final long   FINAL_NONCE;
    private final byte[] DIFFICULTY;
    private long SUCCESS_NONCE;
    private final int ID;
    private final Syncronizer SYNC;
    
    ThreadedMinner(byte[] header, long beginningNonce, long finalNonce, byte[] difficulty, int id, Syncronizer sync){
        BEGINNING_NONCE = beginningNonce;
        FINAL_NONCE = finalNonce;
        DIFFICULTY = difficulty;
        SUCCESS_NONCE = -1;
        ID = id;
        HEADER = new byte[80];
        System.arraycopy(header, 0, HEADER, 0, header.length );
        SYNC = sync;
    }
    
    @Override
    public void run(){
        
        byte[] hash;
        boolean success = false;
        long nonce = BEGINNING_NONCE;
        
        //Work through the current nonce
        while (nonce < FINAL_NONCE) {
            //add the nonce to the byte array
            HEADER[76] = (byte) (nonce & 0xFF);
            HEADER[77] = (byte) ((nonce >> 8) & 0xFF);
            HEADER[78] = (byte) ((nonce >> 16) & 0xFF);
            HEADER[79] = (byte) ((nonce >> 24) & 0xFF);
            
            //Compute the DoulbeShaw256
            hash = MiningUtils.doubleSHA256(HEADER);
            
            //Check to see if the hash is small enough
            for(int c = hash.length - 1; c >=0; c--){
                //we have to mask these values as java automatically casts them to integers
                if ((hash[c] & 0x000000FF) > (DIFFICULTY[c] & 0x000000FF)){
                    break;
                }
                if ((hash[c] & 0x000000FF) < (DIFFICULTY[c] & 0x000000FF)){
                    success = true;
                    break;
                }
            }
            if (success) {break;}
            if (SYNC.isStopped()){break;}
            nonce++;
        }
        
        if (success) {
            SUCCESS_NONCE = nonce;
            System.out.println("Success @ nonce " + SUCCESS_NONCE + "\n successful thread: " + ID);
            SYNC.Stop();
        } else {
            if (!SYNC.isStopped()){
                System.out.println("No successful nonces were found with the given header!");
            }
        }
    }
}

class MiningUtils{
    public static byte[] doubleSHA256(byte[] base) {  
        try{
            //We use DigestUtils because it is thread safe
            MessageDigest digest = DigestUtils.getSha256Digest();
            byte[] hash = digest.digest(digest.digest(base));
            
            return hash;
            
        } catch(Exception ex){
           throw new RuntimeException(ex);
        }
    }
    
    public static String byteArrayToString(byte[] b){
        StringBuffer hexString = new StringBuffer();
        
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(0xff & b[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    public static byte[] reverseByteArray(byte[] array){
        byte[] ret = new byte[array.length];
        for(int c = 0; c < array.length; c++){
            ret[array.length - c - 1] = array[c];
        }
        return ret;
    }
    
    public static byte[] calculateDifficulty(int bits){
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
    
    public static byte[] combineByteArrays(byte[]... arrays){
        int FinalLen = 0;
        for (byte[] b: arrays){
            FinalLen += b.length;
        }
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
    
    public static byte[] toByteArray(String hash){
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
    
    public static byte[] toByteArray(long a){
        return new byte[] {
            (byte) (a & 0xFF),
            (byte) ((a >> 8) & 0xFF),
            (byte) ((a >> 16) & 0xFF),
            (byte) ((a >> 24) & 0xFF)   
        };
    }
    
    public static byte[][] toDoubleByteArray(String[] StringArray){
        byte[][] ret = new byte[StringArray.length][StringArray[0].length()];
        int index = 0;
        for (String s : StringArray) {
            ret[index] = toByteArray(s); 
            index++;
        }
        return ret;
    }
    
    public static String generateRandomHash(int length){
      Random rand = new Random();
      String ReturnVal = "";
      String abc = "abcdef1234567890";
      for (int c = 0; c < length; c++){
        char letter = abc.charAt(rand.nextInt(abc.length()));
        ReturnVal += letter;
      }
      return ReturnVal;
    }
    
    public static byte[][] mergeArrays(byte[][]... ByteArrays){
        int FinalColumnCount = 0;
        for (byte[][] ByteArray : ByteArrays) {
            FinalColumnCount += ByteArray.length;
        }
        byte[][] ret = new byte[FinalColumnCount][ByteArrays[0][0].length];
        int index = 0;
        for (byte[][] ByteArray : ByteArrays){
            for (byte[] Bytes : ByteArray){
                ret[index] = Bytes;
                index++;
            }
        }
        return ret;
    }
    
    public static String[] mergeArrays(String[] a, String[] b) {
        String[] ret = new String[a.length + b.length];
        for(int c = 0; c < ret.length; c++){
            if (c < a.length) {
                ret[c] = a[c];
            } else {
                ret[c] = b[c - a.length];
            }
        }
        return ret;
    }
}

class Header {
    private byte[]   VER;
    private byte[]   PREV_HASH;
    private byte[]   MRKL_ROOT;
    private byte[][] MRKL_BRANCH;
    private byte[][] TX_HASHES;
    private byte[]   COINBASE;
    private byte[]   nTIME;
    private byte[]   nBITS;
    private long     DIFFICULTY;
    
    Header(String[] Transactions, String prev_hash, long ver, long nbits, long ntime){
        VER = MiningUtils.toByteArray(ver);
        nBITS = MiningUtils.toByteArray(nbits);
        DIFFICULTY = nbits;
        nTIME = MiningUtils.toByteArray(ntime);
        PREV_HASH = MiningUtils.toByteArray(prev_hash);
        TX_HASHES = MiningUtils.toDoubleByteArray(Transactions);
        COINBASE = generateCoinBaseByte();
        generateMerklBranchAndRoot(MiningUtils.mergeArrays(
                new byte[][]{COINBASE},
                TX_HASHES), 0);
    }
    
    public void next(){
        COINBASE = generateCoinBaseByte();
        generateMerklRoot(COINBASE, 0);
    }
    
    public byte[] getDifficulty(){
            //https://en.bitcoin.it/wiki/Difficulty
            byte one = (byte) (DIFFICULTY & 0xFF);
            byte two = (byte) ((DIFFICULTY >> 8) & 0xFF);
            byte three = (byte) ((DIFFICULTY >> 16) & 0xFF);
            int exp = (int)DIFFICULTY >> 24;

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
    
    public byte[] getHeader(){
        byte[] Header = MiningUtils.combineByteArrays(
                VER,
                MiningUtils.reverseByteArray(PREV_HASH),
                MiningUtils.reverseByteArray(MRKL_ROOT),
                nTIME,
                nBITS,
                new byte[4]
        ); 
        return Header;
    }
    
    private void generateMerklBranchAndRoot(byte[][] HashList, int index) {
        if (HashList.length == 1){
            MRKL_ROOT = HashList[0];
            return;
        }
        if (index == 0){
            int BranchLength = 0;
            while (Math.pow(2, BranchLength) < HashList.length){
                BranchLength++;
            }
            MRKL_BRANCH = new byte[BranchLength][32];
        }
        
        MRKL_BRANCH[index] = HashList[1];
        byte[][] newHashList;
        if (HashList.length % 2 == 0){
            newHashList = new byte[HashList.length/2][32];
        } else {
            newHashList = new byte[HashList.length/2+1][32];
        }

        for(int i = 0; i < newHashList.length; i++) {
            if (i*2+1 >=  HashList.length){
                newHashList[i] = MiningUtils.reverseByteArray(MiningUtils.doubleSHA256(
                    MiningUtils.combineByteArrays(
                        MiningUtils.reverseByteArray(HashList[i*2]),
                        MiningUtils.reverseByteArray(HashList[i*2])
                )));
            } else {
                newHashList[i] = MiningUtils.reverseByteArray(MiningUtils.doubleSHA256(
                    MiningUtils.combineByteArrays(
                        MiningUtils.reverseByteArray(HashList[i*2]),
                        MiningUtils.reverseByteArray(HashList[i*2+1])
                ))); 
            }
        }
        generateMerklBranchAndRoot(newHashList, index+1);
    }
    
    private void generateMerklRoot(byte[] CurrentHash, int index) {
        if (index >= MRKL_BRANCH.length) {
            MRKL_ROOT = CurrentHash;
            return;
        }
        byte[] newHash = MiningUtils.reverseByteArray(MiningUtils.doubleSHA256(
            MiningUtils.combineByteArrays(
                MiningUtils.reverseByteArray(CurrentHash),
                MiningUtils.reverseByteArray(MRKL_BRANCH[index])
            )));
        generateMerklRoot(newHash, index+1);
    }
    
    private byte[] generateCoinBaseByte(){
        byte[] ret = new byte[32];
        Random rand = new Random();
        rand.nextBytes(ret);
        return ret;
    }
}