package com.RuneLingual.nonLatin.Japanese;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.regex.Pattern;

public class Rom2kat {



    public int inputCount = 0; //for counting number of words in chat input
    public String chatJpMsg = "";//whats written for chat input overlay
    public List<String> kanjKatCandidates = new ArrayList<>();//candidates of kanji or katakana from input, also used for candidates overlay
    public int instCandidateSelection = -1;
    private List<FourValues> japCharDS = new ArrayList<>();//store all japanese words's written form, how its read, type, rank
    private List<String> prevHiraList = new ArrayList<>();//stores the last updated words that are displayed
    private List<String> prevJPList = new ArrayList<>();
    private HashMap<String,String> char2char = new HashMap<>();
    private String notAvailable = "nan";


    public static class FourValues {
        @Getter@Setter
        private String written;
        @Getter@Setter
        private String read;
        @Getter@Setter
        private String type;
        @Getter@Setter
        private int rank;

        public FourValues(String value1, String value2, String value3, int value4) {
            this.written = value1;
            this.read = value2;
            this.type = value3;
            this.rank = value4;
        }
    }

    public String romToKat(String romMsg) {
        StringBuilder katBuilder = new StringBuilder();
        StringBuilder romBuilder = new StringBuilder();
        String pattern = "n[,.!?;:#$%&()'\\s\\d]$";
        String pattern2 = ".+n[,.!?;:#$%&()'\\s\\d]$";

        for (int i = 0; i < romMsg.length(); i++) {
            romBuilder.append(romMsg.charAt(i));
            String romBuffer = romBuilder.toString();
            int romBufferSize = romBuffer.length();
            String katCandidate;

            if (romBufferSize == 0)//something went wrong
                return "";
            if (romBufferSize == 1) {
                katCandidate = romBuffer;
                if (char2char.containsKey(katCandidate)) {
                    String ch = char2char.get(katCandidate);
                    katBuilder.append(ch);
                    romBuilder.setLength(0);
                    continue;
                }
                if (romBuffer.equals("n") && i == romMsg.length() - 1){
                    katBuilder.append("ん");
                    romBuilder.setLength(0);
                    continue;
                }
            } else if (romBufferSize == 2) {

                katCandidate = romBuffer;//eg: ka > カ
                if (char2char.containsKey(katCandidate)) {
                    String ch = char2char.get(katCandidate);
                    katBuilder.append(ch);
                    romBuilder.setLength(0);
                    continue;
                }
                katCandidate = romBuffer.substring(romBufferSize-1);
                if (char2char.containsKey(katCandidate)) {//eg:qe > qエ
                    String ch = char2char.get(katCandidate);
                    Character secToLast = romBuffer.charAt(0);
                    if (Pattern.matches(pattern, romBuffer)) //when n comes before a symbol or space, change it to ン
                        katBuilder.append("ん");
                    else
                        katBuilder.append(secToLast);//append q
                    katBuilder.append(ch); // append エ
                    romBuilder.setLength(0);
                    continue;
                }
                if (Pattern.matches(pattern, romBuffer)){//when n comes before a symbol or space, change it to ン
                    katBuilder.append("ん");
                    katBuilder.append(romBuffer.charAt(1));
                    romBuilder.setLength(0);
                    continue;
                }
            } else {//rombuffer size > 2
                if (Pattern.matches(pattern2, romBuffer)){//when n comes before a symbol or space, change it to ン
                    katBuilder.append(romBuffer, 0, romBufferSize-2);
                    katBuilder.append("ん");
                    String lastChar = Character.toString(romBuffer.charAt(romBufferSize-1));
                    katBuilder.append(char2char.getOrDefault(lastChar, lastChar));
                    romBuilder.setLength(0);
                    continue;
                }
                katCandidate = romBuffer.substring(romBufferSize-3);
                if (char2char.containsKey(katCandidate)) {
                    String ch = char2char.get(katCandidate);
                    if (romBufferSize > 3) {
                        if (romBuffer.charAt(romBufferSize - 4)
                                == romBuffer.charAt(romBufferSize - 3)){ // eg: xwwhe > xッウェ
                            katBuilder.append(romBuffer,0,romBufferSize-4);//append x
                            katBuilder.append("っ");//append ッ
                            katBuilder.append(ch); // append ウェ
                            romBuilder.setLength(0);
                            continue;
                        }
                        if(romBuffer.charAt(romBufferSize - 4) == 'n'){// eg: xnwhe > xンウェ
                            katBuilder.append(romBuffer,0,romBufferSize-4);//append x
                            katBuilder.append("ん");//append ン
                            katBuilder.append(ch); // append ウェ
                            romBuilder.setLength(0);
                            continue;
                        }

                    }//eg:xxxwhe > xxxウェ
                    katBuilder.append(romBuffer, 0, romBufferSize-3);//append xxx
                    katBuilder.append(ch); // append ウェ
                    romBuilder.setLength(0);
                    continue;
                }
                katCandidate = romBuffer.substring(romBufferSize - 2);
                if (char2char.containsKey(katCandidate)) {
                    String ch = char2char.get(katCandidate);
                    if (romBuffer.charAt(romBufferSize - 3)
                            == romBuffer.charAt(romBufferSize - 2)){ // eg: xkka > xッカ
                        katBuilder.append(romBuffer,0,romBufferSize-3);//append x
                        katBuilder.append("っ");//append ッ
                        katBuilder.append(ch); // append ウェ
                        romBuilder.setLength(0);
                        continue;
                    }
                    if(romBuffer.charAt(romBufferSize - 3) == 'n'){// eg: xnka > xンカ
                        katBuilder.append(romBuffer,0,romBufferSize-3);//append x
                        katBuilder.append("ん");//append ン
                        katBuilder.append(ch); // append カ
                        romBuilder.setLength(0);
                        continue;
                    }
                    //eg: xxka > xxカ
                    katBuilder.append(romBuffer, 0, romBufferSize-2);//append xx
                    katBuilder.append(ch); //append カ
                    romBuilder.setLength(0);
                    continue;
                }
                katCandidate = romBuffer.substring(romBufferSize - 1);
                if (char2char.containsKey(katCandidate)) {
                    String ch = char2char.get(katCandidate);
                    katBuilder.append(romBuffer, 0, romBufferSize-1);//append
                    katBuilder.append(ch);
                    romBuilder.setLength(0);
                }
            }
        }
        katBuilder.append(romBuilder.toString());
        return katBuilder.toString();
    }

}
