package com.RuneLingual.SidePanelComponents;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import lombok.Getter;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class ChatBoxSection {
    private SidePanel sidePanel;
    private RuneLingualPlugin plugin;
    @Getter
    private String tabNameGame = "Game";
    @Getter
    private String tabNamePublic = "Public";
    @Getter
    private String tabNameChannel = "Channel";
    @Getter
    private String tabNameClan = "Clan";
    @Getter
    private String tabNameGIM = "GIM";
    JTabbedPane tabbedPane = new JTabbedPane();

    public ChatBoxSection(SidePanel sideP, LangCodeSelectableList langList, RuneLingualPlugin plugin) {
        this.sidePanel = sideP;
        this.plugin = plugin;

        translateTabNames(langList);

        addTab(tabbedPane, tabNameGame);
        addTab(tabbedPane, tabNamePublic);
        addTab(tabbedPane, tabNameChannel);
        addTab(tabbedPane, tabNameClan);
        addTab(tabbedPane, tabNameGIM);

        sidePanel.add(tabbedPane);
        sidePanel.setSize(400, 300);
        sidePanel.setVisible(true);
    }

    private static void addTab(JTabbedPane tabbedPane, String title) {
        JTextArea textArea = new JTextArea();
//        for (String sentence : sentences) {
//            textArea.append(sentence + "\n");
//        }
        textArea.setEditable(false); // This line makes the text uneditable
        JScrollPane scrollPane = new JScrollPane(textArea);
        tabbedPane.addTab(title, scrollPane);
    }

    public void addSentenceToTab(String tabTitle, String sentence) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getTitleAt(i).equals(tabTitle)) {
                JScrollPane scrollPane = (JScrollPane) tabbedPane.getComponentAt(i);
                JViewport viewport = scrollPane.getViewport();
                JTextArea textArea = (JTextArea) viewport.getView();
                textArea.append(sentence + "\n");
                return;
            }
        }
        System.out.println("No tab found with title: " + tabTitle);
    }


    private void translateTabNames(LangCodeSelectableList targetLanguage) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            tabNameGame = "Game";
            tabNamePublic = "Public";
            tabNameChannel = "Channel";
            tabNameClan = "Clan";
            tabNameGIM = "GIM";
        } else if (targetLanguage == LangCodeSelectableList.PORTUGUÊS_BRASILEIRO) {
            tabNameGame = "Jogo";
            tabNamePublic = "Público";
            tabNameChannel = "Canal";
            tabNameClan = "Clã";
            tabNameGIM = "GIM";
        } else if (targetLanguage == LangCodeSelectableList.NORSK) {
            tabNameGame = "Spill";
            tabNamePublic = "Offentlig";
            tabNameChannel = "Kanal";
            tabNameClan = "Klan";
            tabNameGIM = "GIM";
        } else if (targetLanguage == LangCodeSelectableList.日本語) {
            tabNameGame = "ゲーム";
            tabNamePublic = "公共";
            tabNameChannel = "チャンネル";
            tabNameClan = "クラン";
            tabNameGIM = "GIM";
        }// todo: add more here as languages are added
    }
}
