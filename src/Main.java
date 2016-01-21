import com.beust.jcommander.JCommander;
import com.google.common.base.Joiner;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws Exception {
        ArgParser argParser = new ArgParser();
        new JCommander(argParser, args);
        if (argParser.debug) {
            System.out.printf("Debugging mode on\n%-22s%s\n%-22s%s\n",
                    "File to convert:",
                    argParser.file,
                    "Output folder:",
                    argParser.outDirString);
        }

        Pattern pattern = Pattern.compile("_haps\\.gz$");
        String fileStem = pattern.matcher(argParser.file).replaceAll("");
        System.out.println("File stem: " + fileStem);
        System.out.println("File : " + argParser.file);
        File outDir;
        File machOutFile = new File(fileStem + ".mach.out");
        File snpsOutFile = new File(fileStem + ".data.dat");
        if(argParser.outDirString.isEmpty()) throw new Exception("outDir option missing.");
        outDir = new File(argParser.outDirString);
        outDir.mkdir();
        machOutFile = new File(outDir, machOutFile.getName());
        snpsOutFile = new File(outDir, snpsOutFile.getName());
        String[] fidiid = readSamples(fileStem);


        // get the HAPL01 / HAPL02 rows
        String[] hapl = new String[fidiid.length];
        for(int i=0; i<hapl.length; i++) {
            if(i % 2 == 0) {
                hapl[i] = "HAPL01";
            } else {
                hapl[i] = "HAPL02";
            }
        }

        ArrayList<String[]> outputLines = new ArrayList<String[]>();
        outputLines.add(fidiid);
        outputLines.add(hapl);
        ArrayList<String> snps = readHapsFile(fileStem, outputLines);


        // Transpose the output matrix
        int nSnp = outputLines.size();
        int nObserve = fidiid.length;
        String[][] outputTranspose = new String[nObserve][nSnp];
        for(int i=0; i<nSnp; i++) {
            for(int j=0; j<nObserve; j++) {
                outputTranspose[j][i] = outputLines.get(i)[j];
            }
        }

        // get rows of strings
        String[] outputStrings = new String[nObserve];
        for(int i=0; i<nObserve; i++) {
            String[] idAndHapl = Arrays.copyOfRange(outputTranspose[i], 0, 2);
            String idAndHaplString = Joiner.on(" ").join(idAndHapl);
            String[] genotypes = Arrays.copyOfRange(outputTranspose[i], 2, outputTranspose[i].length);
            String genotypesString = Joiner.on("").join(genotypes);
            outputStrings[i] = idAndHaplString + " " + genotypesString;
        }

        String machOutString = Joiner.on("\n").join(outputStrings);
        BufferedWriter machOutBuffer = null;
        try {
            machOutBuffer = new BufferedWriter(new FileWriter(machOutFile));
            machOutBuffer.write(machOutString);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            machOutBuffer.close();
        }

        String snpsString = Joiner.on("\n").join(snps);
        BufferedWriter snpsOutBuffer = null;
        try {
            snpsOutBuffer = new BufferedWriter(new FileWriter(snpsOutFile));
            snpsOutBuffer.write(snpsString);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            snpsOutBuffer.close();
        }
    }

    private static ArrayList<String> readHapsFile(String fileStem, ArrayList<String[]> outputLines) throws Exception {
        // read the _haps file
        File hapsFile   = new File(fileStem + "_haps");
        BufferedReader hapsBuffer = new BufferedReader(new FileReader(hapsFile));
        ArrayList<String> snps = new ArrayList<String>();
        String hapsLine;
        String tmpGen;
        while((hapsLine = hapsBuffer.readLine()) != null) {
            String[] cols = hapsLine.split(" +");
            String snp = "M\t" + cols[1];
            snps.add(snp);
            String refGen = cols[3];
            String mutGen = cols[4];
            String[] genCols = Arrays.copyOfRange(cols, 5, cols.length);
            String[] genColsTrans = new String[genCols.length];

            if(refGen.length() == 1 && mutGen.length() == 1) {
                for(int i=0; i<genCols.length; i++) {
                    genColsTrans[i] = (genCols[i].equals("0")) ? refGen : mutGen;
                }
            } else if(refGen.length() > 1 && mutGen.length() == 1) {
                for(int i=0; i<genCols.length; i++) {
                    genColsTrans[i] = (genCols[i].equals("0")) ? "R" : "D";
                }
            } else if(refGen.length() == 1 && mutGen.length() > 1) {
                for(int i=0; i<genCols.length; i++) {
                    genColsTrans[i] = (genCols[i].equals("0")) ? "R" : "I";
                }
            } else {
                throw new Exception("Impute2 file mal-formatted.");
            }
            outputLines.add(genColsTrans);
        }
        return snps;
    }

    private static String[] readSamples(String fileStem) throws IOException {
        // Read the _samples file
        String sampleLine;
        ArrayList<String> fidiidList = new ArrayList<String>();
        try(BufferedReader sampleBuffer = new BufferedReader(new FileReader(new File(fileStem + "_samples")))) {
            sampleBuffer.readLine();
            sampleBuffer.readLine();
            while ((sampleLine = sampleBuffer.readLine()) != null) {
                String[] cols = sampleLine.split(" +", 3);
                String combinedLine = cols[0] + "->" + cols[1];
                fidiidList.add(combinedLine);
                fidiidList.add(combinedLine);
            }
        }
        return fidiidList.toArray(new String[fidiidList.size()]);
    }
}
