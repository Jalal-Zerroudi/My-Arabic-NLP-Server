package MainPackNLP;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// SAFAR imports
import safar.basic.morphology.stemmer.factory.StemmerFactory;
import safar.basic.morphology.stemmer.interfaces.IStemmer;
import safar.basic.morphology.stemmer.model.WordStemmerAnalysis;

public class MapGenerator {

    private static final String DATE_SUFFIX = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date());
    private static final String OUTPUT_DIR = "Map_Out";
    private static final String DEFAULT_STOP_WORDS_PATH = "src/data/AR_Stop_Words.json";

    // ----------------------------------------------------------------------
    // Génération de Map1 : Fréquence des mots arabes
    // ----------------------------------------------------------------------
    public static void generateFrequencyMap(List<File> selectedFiles, String defaultFolder) throws IOException {
        Map<String, Map<String, Integer>> map1 = new HashMap<>();
        Files.createDirectories(Paths.get(OUTPUT_DIR, "Frequence"));

        Set<String> stopWords = loadArabicStopWords(DEFAULT_STOP_WORDS_PATH);
        System.out.println("✅ Stop words chargés : " + stopWords.size());

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            Files.walk(Paths.get(defaultFolder))
                    .filter(Files::isRegularFile)
                    .forEach(path -> processFileForFrequency(map1, path, stopWords));
        } else {
            for (File file : selectedFiles) {
                processFileForFrequency(map1, file.toPath(), stopWords);
            }
        }

        String outputPath = OUTPUT_DIR + "/Frequence/Map1_Frequence_" + DATE_SUFFIX + ".json";
        writeJson(outputPath, map1);
        System.out.println("✅ Map1 généré : " + outputPath);
    }

    // ----------------------------------------------------------------------
    // Traitement individuel d’un fichier pour Map1
    // ----------------------------------------------------------------------
    private static void processFileForFrequency(Map<String, Map<String, Integer>> map, Path path, Set<String> stopWords) {
        try {
            String content = new String(Files.readAllBytes(path), "UTF-8");
            content = content.replaceAll("[^\\p{InArabic}\\s]", " "); // Garde uniquement arabe et espaces
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

    // ----------------------------------------------------------------------
    // Génération de Map2 : Racinisation (Stemming)
    // ----------------------------------------------------------------------
    public static void generateStemmingMap(String map1FilePath) throws IOException {
        // Si le fichier Map1 n’existe pas → génération automatique
        File file = new File(map1FilePath);
        if (!file.exists()) {
            System.out.println("⚠️ Aucun Map1 trouvé, génération automatique...");
            generateFrequencyMap(null, "src/data");
            // retrouver le dernier Map1
            File folder = new File(OUTPUT_DIR + "/Frequence");
            File[] jsons = folder.listFiles((d, n) -> n.startsWith("Map1_Frequence"));
            if (jsons == null || jsons.length == 0) {
                throw new FileNotFoundException("Impossible de créer Map1 automatiquement.");
            }
            file = Arrays.stream(jsons).max(Comparator.comparingLong(File::lastModified)).get();
            map1FilePath = file.getPath();
        }

        // Charger Map1
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(map1FilePath));
        Map<String, Map<String, Double>> map1 = gson.fromJson(reader, Map.class);
        reader.close();

        Map<String, Map<String, Integer>> map2 = new HashMap<>();
        Files.createDirectories(Paths.get(OUTPUT_DIR, "Stemming"));

        IStemmer stemmer = StemmerFactory.getLight10Implementation();

        for (String filePath : map1.keySet()) {
            Map<String, Integer> stems = new HashMap<>();
            Map<String, Double> words = map1.get(filePath);

            for (String word : words.keySet()) {
                String normalizedWord = normalizeArabic(word);
                try {
                    List<WordStemmerAnalysis> analyses = stemmer.stem(normalizedWord);
                    String stem = extractStemSafar(analyses, normalizedWord);
                    stem = cleanSafarOutput(stem);
                    stems.put(stem, stems.getOrDefault(stem, 0) + 1);
                } catch (Exception e) {
                    stems.put(normalizedWord, stems.getOrDefault(normalizedWord, 0) + 1);
                }
            }

            map2.put(filePath, stems);
            System.out.println("🌿 Racinisation terminée pour : " + filePath);
        }

        String outputPath = OUTPUT_DIR + "/Stemming/Map2_Stemming_" + DATE_SUFFIX + ".json";
        writeJson(outputPath, map2);
        System.out.println("✅ Map2 généré : " + outputPath);
    }

    // ----------------------------------------------------------------------
    // Extraction du stem depuis l’objet SAFAR
    // ----------------------------------------------------------------------
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

    // ----------------------------------------------------------------------
    // Nettoyage du format SAFAR "{type= STEM, morpheme= ...}"
    // ----------------------------------------------------------------------
    private static String cleanSafarOutput(String text) {
        if (text == null) return "";
        text = text.replaceAll("\\{.*morpheme\\s*=\\s*", "");
        text = text.replaceAll("}", "");
        return normalizeArabic(text.trim());
    }

    // ----------------------------------------------------------------------
    // Normalisation du texte arabe (unification orthographique)
    // ----------------------------------------------------------------------
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

    // ----------------------------------------------------------------------
    // Lecture du fichier JSON des stop words arabes
    // ----------------------------------------------------------------------
    private static Set<String> loadArabicStopWords(String path) {
        try {
            Gson gson = new Gson();
            String json = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
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

    // ----------------------------------------------------------------------
    // Écriture JSON formatée joliment
    // ----------------------------------------------------------------------
    private static void writeJson(String filename, Object map) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(filename)) {
            gson.toJson(map, writer);
        }
    }
}
