package com.RuneLingual.nonLatin.Japanese;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
public class Rom2hira {

    public int inputCount = 0; //for counting number of words in chat input
    public String chatJpMsg = "";//whats written for chat input overlay
    public List<String> kanjiCandidates = new ArrayList<>();//candidates of kanji or katakana from input, also used for candidates overlay
    public int instCandidateSelection = -1;
    private List<FourValues> japCharDS = new ArrayList<>();//store all japanese words's written form, how its read, type, rank
    private List<String> prevHiraList = new ArrayList<>();//stores the last updated words that are displayed
    private List<String> prevJPList = new ArrayList<>();
    private HashMap<String,String> char2char = new HashMap<>();
    private String notAvailable = "nan";

    @Inject
    Rom2hira() {
        String charDir = FileNameAndPath.getLocalBaseFolder() + "/" +
                LangCodeSelectableList.日本語.getLangCode() +
                "/latin2foreign_" + LangCodeSelectableList.日本語.getLangCode() + ".txt";

        char2char = new HashMap<>();
        putCharToHash(char2char, charDir);
    }

    private void putCharToHash(HashMap<String, String> hash, String dirName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader
                (new FileInputStream(dirName), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    hash.put(parts[0].trim().toLowerCase(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            log.info("error creating hashmap for transform dict, for type : " + dirName);
            e.printStackTrace();
        }
    }


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

        List<String> escapePattern = Arrays.asList("::",";;",";:",":;");// escape sequence for latin character input
        boolean escaping = false;

        for (int i = 0; i < romMsg.length(); i++) {
            romBuilder.append(romMsg.charAt(i));
            String romBuffer = romBuilder.toString();
            int romBufferSize = romBuffer.length();
            String katCandidate;

            if (escaping) {
                katBuilder.append(romMsg.charAt(i));
                romBuilder.setLength(0);
                if(katBuilder.length() > 1 &&
                        escapePattern.contains(katBuilder.substring(katBuilder.length()-2))){
                    escaping = false;
                    katBuilder.setLength(katBuilder.length()-2);
                    katBuilder.append(" ");
                    continue;
                } else {
                    continue;
                }
            }

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
                if(escapePattern.contains(romBuffer)){
                    romBuilder.setLength(0);
                    katBuilder.append(" ");
                    escaping = true;
                    continue;
                }

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
                if(escapePattern.contains(romBuffer.substring(romBufferSize-2))){
                    romBuilder.setLength(0);
                    escaping = true;
                    continue;
                }

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
        katBuilder.append(romBuilder);
        return katBuilder.toString();
    }

}
