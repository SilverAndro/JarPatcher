package dev.silverandro.jar_patcher;

import me.coley.cafedude.InvalidClassException;
import me.coley.cafedude.classfile.ClassFile;
import me.coley.cafedude.io.ClassFileReader;
import me.coley.cafedude.io.ClassFileWriter;
import me.coley.cafedude.transform.IllegalStrippingTransformer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Main {
    public static void main(String[] args) throws IOException, InvalidClassException {
        if (args.length == 0) {
            System.out.println("Please pass at least 1 jar file to patch");
        }

        for (String jar : args) {
            JarFile inputFile = new JarFile(new File(jar));

            Path outputFile = Paths.get(jar).getParent().resolve(jar.replace(".jar", "-patched.jar"));
            outputFile.toFile().createNewFile();
            JarOutputStream output = new JarOutputStream(Files.newOutputStream(outputFile), new Manifest());

            Enumeration<JarEntry> inputEntries = inputFile.entries();

            while (inputEntries.hasMoreElements()) {
                JarEntry nextEntry = inputEntries.nextElement();

                if (nextEntry.getName().equals("META-INF/MANIFEST.MF")) continue;

                InputStream inputStream = inputFile.getInputStream(nextEntry);
                byte[] entryBytes = readAllBytes(inputStream);
                inputStream.close();

                if (nextEntry.getName().endsWith(".class")) {
                    ClassFileReader cr = new ClassFileReader();
                    ClassFile cf = cr.read(entryBytes);
                    new IllegalStrippingTransformer(cf).transform();
                    entryBytes = new ClassFileWriter().write(cf);
                }

                output.putNextEntry(nextEntry);
                output.write(entryBytes);
                output.closeEntry();
            }

            output.close();
        }
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int readByte;
        while ((readByte = is.read()) != -1) {
            buffer.write(readByte);
        }

        return buffer.toByteArray();
    }
}
