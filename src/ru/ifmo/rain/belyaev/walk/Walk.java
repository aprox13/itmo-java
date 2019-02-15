package ru.ifmo.rain.belyaev.walk;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Walk {


    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Program need 2 non-null input arguments: input_file and output file");
            return;
        }

        Path inPath, outPath;


        String input = args[0];
        String output = args[1];


        try {
            inPath = Paths.get(input);
        } catch (InvalidPathException e) {
            System.out.println("Wrong input file path '" + input + "'");
            return;
        }

        try {
            outPath = Paths.get(output);
        } catch (InvalidPathException e) {
            System.out.println("Wrong output file path '" + output + "'");
            return;
        }

        if (outPath.getParent() != null) {
            try {
                Files.createDirectories(outPath.getParent());
            } catch (IOException e) {
                System.out.println("Unable to create folder for output file");
                return;
            }
        }


        try (BufferedReader reader = Files.newBufferedReader(inPath, StandardCharsets.UTF_8)) {
            try (BufferedWriter writer = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        writer.write(getHash(line));
                    } catch (IOException e) {
                        System.out.println("Something went wrong while writing to output file.");
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Something went wrong while '" + output + "' was opened");
            }
        } catch (IOException e) {
            System.out.println("Something went wrong while '" + input + "' was opened");
        }
    }

    private static String getHash(final String pathLine) {
        int hash = 0x811c9dc5;
        try {
            BufferedInputStream is = new BufferedInputStream(Files.newInputStream(Paths.get(pathLine)));
            byte[] bytes = new byte[1024];
            int cnt;
            while ((cnt = is.read(bytes)) != -1) {
                for (int i = 0; i < cnt; i++) {
                    hash = (hash * 0x01000193) ^ (bytes[i] & 0xff);
                }
            }
        } catch (InvalidPathException | IOException e) {
            return zeroHash(pathLine);
        }
        return formatHash(hash, pathLine);
    }


    private static String formatHash(final int hash, final String path) {
        return String.format("%08x", hash) + " " + path + System.lineSeparator();
    }

    private static String zeroHash(final String path) {
        return formatHash(0, path);
    }

}
