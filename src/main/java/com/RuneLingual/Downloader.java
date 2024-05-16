package com.RuneLingual;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import javax.inject.Inject;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class Downloader {//downloads translations and japanese char images to external file
    @Inject
    private RuneLingualPlugin plugin;

    public static File localBaseFolder;
    private String GITHUB_BASE_URL;
    private String langCode;

    public void initDownloader(String langCodeGiven) {
        langCode = "jp";//todo: replace with langCodeGiven after rling transcripts are ready
        localBaseFolder = new File(RuneLite.RUNELITE_DIR.getPath() + File.separator + "RuneLingual_resources");
        createDir(localBaseFolder.getPath() + File.separator + langCode);
        String LOCAL_HASH_NAME = langCode + File.separator + "hashListLocal_" + langCode + ".txt";
        String REMOTE_HASH_FILE = "https://raw.githubusercontent.com/YS-jack/Runelingual-Transcripts/original-main/draft/" + langCode + "/hashList_" + langCode + ".txt";
        GITHUB_BASE_URL = "https://raw.githubusercontent.com/YS-jack/Runelingual-Transcripts/original-main/";
        try {
            Path dirPath = Paths.get(localBaseFolder.getPath());
            if (!Files.exists(dirPath)) {
                try {
                    // Attempt to create the directory
                    Files.createDirectories(dirPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Map<String, String> localHashes = readHashFile(Paths.get(localBaseFolder.getPath(), LOCAL_HASH_NAME));
            Map<String, String> remoteHashes = readHashFile(new URL(REMOTE_HASH_FILE));

            for (Map.Entry<String, String> entry : remoteHashes.entrySet()) {
                String localHash = localHashes.get(entry.getKey());
                String remoteHash = entry.getValue();
                String remoteDir = entry.getKey();
                Path localPath = Paths.get(localBaseFolder.getPath(), remoteDir.replace("draft\\",""));

                if (localHash == null || !localHash.equals(remoteHash) ||
                        (!remoteDir.startsWith("char") && Files.notExists(localPath))) {
                    log.info("hash value not same as remote: " + entry.getKey());
                    downloadAndUpdateFile(entry.getKey());
                }
            }

            // Overwrite local hash file with the updated remote hash file
            Files.copy(new URL(REMOTE_HASH_FILE).openStream(), Paths.get(localBaseFolder.getPath(), LOCAL_HASH_NAME), StandardCopyOption.REPLACE_EXISTING);

//            //create webhook dir if none
//            createDir(localBaseFolder.getPath() + "/webhookSent");
//            String[] webhookType = {"sentAPITranslationMsg.txt","sentGameMsgAndDialog.txt",
//                    "sentItemAndWidgetsName.txt","sentMenuOptions.txt", "sentNpcName.txt", "sentObjName.txt"};
//            for(String filename:webhookType) {
//                Path path = Paths.get(localBaseFolder.getPath() + File.separator + "webhookSent" + File.separator + filename);
//                if (!Files.exists(path))
//                    Files.createFile(path);
//            }

        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }

    }
    private  void createDir(String path){
        Path dirPath = Paths.get(path);
        if (!Files.exists(dirPath)) {
            try {
                // Attempt to create the directory
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private  Map<String, String> readHashFile(Path filePath) throws IOException {
        Map<String, String> hashes = new HashMap<>();
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    hashes.put(parts[0], parts[1]);
                }
            }
        }
        return hashes;
    }

    private  Map<String, String> readHashFile(URL fileUrl) throws IOException {
        Map<String, String> hashes = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileUrl.openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    hashes.put(parts[0], parts[1]);
                }
            }
        }
        return hashes;
    }

    private  void downloadAndUpdateFile(String filePath) throws IOException {
        // filePath example: "draft\jp\actions_jp.json" this is the location of files relative to the GitHub repo root of RuneLite-Transcripts
        log.info("updating file " + filePath);
        URL fileUrl = new URL(GITHUB_BASE_URL + filePath.replace("\\","/"));
        Path localPath = Paths.get(localBaseFolder.getPath(), filePath.replace("draft\\",""));

        if (filePath.startsWith("char") && Files.notExists(localPath)) {
            Files.createDirectories(localPath);
        }

        // Check if the char directory exists, if not, create it
        if (!filePath.startsWith("char") && Files.notExists(localPath.getParent())) {
            Files.createDirectories(localPath.getParent());
        }

        // Special handling for the 'char' directory
        if (filePath.startsWith("char") && Files.isDirectory(localPath)) {
            Files.walkFileTree(localPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }


        // Download and replace the file
        if (!filePath.startsWith("char")) {
            Files.copy(fileUrl.openStream(), localPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            localPath = Paths.get(localBaseFolder.getPath(), "char_" + langCode + ".zip");
            //localPath = Paths.get("/com/japanese/char.zip");
            updateCharDir(localPath);

        }
    }
    public  void  updateCharDir(Path localPath) throws IOException {
        URL fileUrl3 = new URL(GITHUB_BASE_URL + "/draft/" + langCode + "/char_" + plugin.getTargetLanguage().getLangCode() + "zip");
        Files.copy(fileUrl3.openStream(), localPath, StandardCopyOption.REPLACE_EXISTING);
        unzip(String.valueOf(localPath), localBaseFolder.getPath());
        Files.delete(localPath);
    }

    public  void unzip(String zipFilePath, String destDir) {
        File dir = new File(destDir);
        // create output directory if it doesn't exist
        if (!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                //create directories for sub directories in zip
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                //close this ZipEntry
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public  void addFilesToJar(File source, File[] filesToAdd) {
        try {
            // Temporary file for the new JAR
            File tempJarFile = File.createTempFile("charTemp", ".jar");

            // Initialize a stream to write to the temporary file
            JarOutputStream tempJar = new JarOutputStream(new FileOutputStream(tempJarFile));

            // Optionally, set the manifest file (if you want to modify or set properties)
//            Manifest manifest = new Manifest();
//            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
//            tempJar.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
//            manifest.write(tempJar);

            // Buffer for file reading
            byte[] buffer = new byte[1024];
            int bytesRead;

            // Process each file to add
            for (File file : filesToAdd) {
                if (file == null || !file.exists() || file.isDirectory())
                    continue; // Skip directories and non-existing files

                try (FileInputStream fis = new FileInputStream(file)) {
                    // Create a new entry for the file in the JAR
                    JarEntry entry = new JarEntry(file.getName());
                    tempJar.putNextEntry(entry);

                    // Read the file and write it to the JAR
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        tempJar.write(buffer, 0, bytesRead);
                    }
                }
            }

            tempJar.close();

            // Optionally, if you want to update an existing JAR file
            if (source.exists()) {
                source.delete(); // Delete the original JAR
            }

            // Rename the new JAR to the original name
            if (!tempJarFile.renameTo(source)) {
                throw new IOException("Could not rename the temporary file");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
