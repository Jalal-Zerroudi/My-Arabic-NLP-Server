package MainPackNLP;

import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.*;

public class safar_classes {

    public static void main(String[] args) throws Exception {
        String packageName = "safar"; // Package racine SAFAR
        Set<Class<?>> classes = getClasses(packageName);

        if (classes.isEmpty()) {
            System.out.println("⚠️ Aucune classe trouvée dans le package " + packageName);
            return;
        }

        // Liste finale (classe → méthodes)
        List<Map<String, Object>> result = new ArrayList<>();

        for (Class<?> cls : classes) {
            try {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("class", cls.getName());

                List<String> methods = new ArrayList<>();
                for (Method m : cls.getDeclaredMethods()) {
                    String signature = Modifier.toString(m.getModifiers()) + " "
                                     + m.getReturnType().getSimpleName() + " "
                                     + m.getName() + "(";

                    Class<?>[] params = m.getParameterTypes();
                    for (int i = 0; i < params.length; i++) {
                        signature += params[i].getSimpleName();
                        if (i < params.length - 1) signature += ", ";
                    }
                    signature += ")";
                    methods.add(signature);
                }

                entry.put("methods", methods);
                result.add(entry);

            } catch (Throwable e) {
                // ignore les classes non chargeables
            }
        }

        // Export JSON simple
        exportJson(result, "safar_classes_methods.json");
        System.out.println("✅ Fichier généré : safar_classes_methods.json");
    }

    /** 🔍 Récupère toutes les classes d’un package (dans un dossier ou jar) */
    public static Set<Class<?>> getClasses(String packageName) throws IOException {
        Set<Class<?>> classes = new HashSet<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = loader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            if ("file".equals(protocol)) {
                File directory = new File(resource.getFile());
                classes.addAll(findClasses(directory, packageName));
            } else if ("jar".equals(protocol)) {
                try {
                    JarURLConnection conn = (JarURLConnection) resource.openConnection();
                    JarFile jar = conn.getJarFile();
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith(path) && name.endsWith(".class") && !entry.isDirectory()) {
                            String className = name.replace('/', '.').substring(0, name.length() - 6);
                            try {
                                classes.add(Class.forName(className));
                            } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }
        return classes;
    }

    /** 🔁 Parcours récursif d’un dossier */
    private static Set<Class<?>> findClasses(File dir, String packageName) {
        Set<Class<?>> classes = new HashSet<>();
        if (!dir.exists()) return classes;

        File[] files = dir.listFiles();
        if (files == null) return classes;

        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(className));
                } catch (Throwable ignored) {}
            }
        }
        return classes;
    }

    /** 🧾 Export JSON minimaliste */
    private static void exportJson(List<Map<String, Object>> list, String fileName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("[");
            for (int i = 0; i < list.size(); i++) {
                Map<String, Object> item = list.get(i);
                writer.print("  { \"class\": \"" + item.get("class") + "\", \"methods\": [");
                List<String> methods = (List<String>) item.get("methods");
                for (int j = 0; j < methods.size(); j++) {
                    writer.print("\"" + methods.get(j).replace("\"", "\\\"") + "\"");
                    if (j < methods.size() - 1) writer.print(", ");
                }
                writer.print("] }");
                if (i < list.size() - 1) writer.println(",");
                else writer.println();
            }
            writer.println("]");
        } catch (IOException e) {
            System.err.println("Erreur JSON : " + e.getMessage());
        }
    }
}
