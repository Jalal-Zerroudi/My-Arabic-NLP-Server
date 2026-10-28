package MainPackNLP;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// SAFAR imports
import safar.basic.morphology.stemmer.factory.StemmerFactory;
import safar.basic.morphology.stemmer.interfaces.IStemmer;
import safar.basic.morphology.stemmer.model.WordStemmerAnalysis;

public class MapGenerator {

    private static final String OUTPUT_DIR = "Map_Out";
    private static final String DEFAULT_STOP_WORDS_PATH = "src/data/AR_Stop_Words.json";

    private static String createDateSuffix() {
        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
    }

    public static void generateFrequencyMap(List<File> selectedFiles, String defaultFolder) throws IOException {
        Map<String, Map<String, Integer>> map1 = new HashMap<>();
        Files.createDirectories(Paths.get(OUTPUT_DIR, "Frequence"));

        Set<String> stopWords = loadArabicStopWords(DEFAULT_STOP_WORDS_PATH);
        System.out.println("✅ Stop words chargés : " + stopWords.size());

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            try (Stream<Path> paths = Files.walk(Paths.get(defaultFolder))) {
                paths.filter(Files::isRegularFile)
                        .forEach(path -> processFileForFrequency(map1, path, stopWords));
            }
        } else {
            for (File file : selectedFiles) {
                processFileForFrequency(map1, file.toPath(), stopWords);
            }
        }

        String outputPath = OUTPUT_DIR + "/Frequence/Map1_Frequence_" + createDateSuffix() + ".json";
        writeJson(outputPath, map1);
        System.out.println("✅ Map1 généré : " + outputPath);
    }

    private static void processFileForFrequency(Map<String, Map<String, Integer>> map, Path path, Set<String> stopWords) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            content = content.replaceAll("[^\\p{InArabic}\\s]", " ");
            String[] words = content.trim().split("\\s+");

            Map<String, Integer> freq = new HashMap<>();

            for (String w : words) {
                w = normalizeArabic(w);
                if (w.isEmpty()) continue;
                if (w.length() < 2) continue;
                if (stopWords.contains(w)) continue;

                freq.put(w, freq.getOrDefault(w, 0) + 1);
            }

            map.put(path.toString(), freq);
            System.out.println("🧾 Fichier analysé : " + path);

        } catch (Exception e) {
            System.err.println("⚠️ Erreur lecture fichier : " + path + " → " + e.getMessage());
        }
    }

    public static void generateStemmingMap(String map1FilePath) throws IOException {
        File file = new File(map1FilePath);
        if (!file.exists()) {
            System.out.println("⚠️ Aucun Map1 trouvé, génération automatique...");
            generateFrequencyMap(null, "src/data");
            File folder = new File(OUTPUT_DIR + "/Frequence");
            File[] jsons = folder.listFiles((d, n) -> n.startsWith("Map1_Frequence"));
            if (jsons == null || jsons.length == 0) {
                throw new FileNotFoundException("Impossible de créer Map1 automatiquement.");
            }
            file = Arrays.stream(jsons).max(Comparator.comparingLong(File::lastModified)).get();
            map1FilePath = file.getPath();
        }

        Gson gson = new Gson();
        Map<String, Map<String, Double>> map1;
        try (Reader reader = Files.newBufferedReader(Paths.get(map1FilePath), StandardCharsets.UTF_8)) {
            map1 = gson.fromJson(reader, Map.class);
        }

        Map<String, Map<String, Integer>> map2 = new HashMap<>();
        Files.createDirectories(Paths.get(OUTPUT_DIR, "Stemming"));

        IStemmer stemmer = StemmerFactory.getLight10Implementation();

        for (String filePath : map1.keySet()) {
            Map<String, Integer> stems = new HashMap<>();
            Map<String, Double> words = map1.get(filePath);

            for (String word : words.keySet()) {
                String normalizedWord = normalizeArabic(word);
                int frequency = words.get(word).intValue();
                try {
                    List<WordStemmerAnalysis> analyses = stemmer.stem(normalizedWord);
                    String stem = extractStemSafar(analyses, normalizedWord);
                    stem = cleanSafarOutput(stem);
                    stems.put(stem, stems.getOrDefault(stem, 0) + frequency);
                } catch (Exception e) {
                    stems.put(normalizedWord, stems.getOrDefault(normalizedWord, 0) + frequency);
                }
            }

            map2.put(filePath, stems);
            System.out.println("🌿 Racinisation terminée pour : " + filePath);
        }

        String outputPath = OUTPUT_DIR + "/Stemming/Map2_Stemming_" + createDateSuffix() + ".json";
        writeJson(outputPath, map2);
        System.out.println("✅ Map2 généré : " + outputPath);
    }

    private static String extractStemSafar(List<WordStemmerAnalysis> analyses, String originalWord) {
        if (analyses == null || analyses.isEmpty()) return originalWord;

        try {
            Object innerList = analyses.get(0).getListStemmerAnalysis();
            if (innerList instanceof List) {
                List<?> stems = (List<?>) innerList;
                if (!stems.isEmpty()) {
                    return stems.get(0).toString();
                }
            }
        } catch (Exception ignored) {}
        return originalWord;
    }

    private static String cleanSafarOutput(String text) {
        if (text == null) return "";
        text = text.replaceAll("\\{.*morpheme\\s*=\\s*", "");
        text = text.replaceAll("}", "");
        return normalizeArabic(text.trim());
    }

    private static String normalizeArabic(String text) {
        if (text == null) return "";
        return text
                .replaceAll("[إأآا]", "ا")
                .replaceAll("ى", "ي")
                .replaceAll("ؤ", "و")
                .replaceAll("ئ", "ي")
                .replaceAll("ة", "ه")
                .replaceAll("[ًٌٍَُِّْ]", "")
                .trim();
    }

    private static Set<String> loadArabicStopWords(String path) {
        try {
            Gson gson = new Gson();
            String json = Files.readString(Paths.get(path), StandardCharsets.UTF_8);
            Map<String, List<String>> data = gson.fromJson(json, Map.class);
            Set<String> stopWords = new HashSet<>();
            for (String w : data.get("arabic_stopwords")) {
                stopWords.add(normalizeArabic(w));
            }
            return stopWords;
        } catch (Exception e) {
            System.err.println("⚠️ Erreur chargement stop words : " + e.getMessage());
            return new HashSet<>();
        }
    }

    private static void writeJson(String filename, Object map) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(Paths.get(filename), StandardCharsets.UTF_8)) {
            gson.toJson(map, writer);
        }
    }
}
