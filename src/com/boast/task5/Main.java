/*
 * Main.java 15/08/2017
 *
 * Created by Bondarenko Oleh
 */


package com.boast.task5;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.print("Enter directory to search: ");

        String dir;
        while (true) {
            dir = in.nextLine();
            if (new File(dir).exists()) {
                break;
            } else {
                System.out.println("Entered directory not exist");
                System.out.print("Enter directory to search: ");
            }
        }

        TokensRemover tokensRemover = new TokensRemover(new File(dir), 3, 5);
        FutureTask<String[]> task = new FutureTask<>(tokensRemover);
        new Thread(task).start();

        boolean findFlag = false;
        try {
            for (String str : task.get()) {
                findFlag = true;
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        if (!findFlag){
            System.out.print("Nothing found");
        }
    }
}

class TokensRemover implements Callable<String[]>{
    private File dir;
    private int tokenSizeFrom;
    private int tokenSizeTo;

    TokensRemover(File dir, int tokenSizeFrom, int tokenSizeTo){
        this.dir = dir;
        this.tokenSizeFrom = tokenSizeFrom;
        this.tokenSizeTo = tokenSizeTo;
    }

    @Override
    public String[] call() throws Exception {
        ArrayList<String> resultArray = new ArrayList<>();
        try {
            File[] files = dir.listFiles();
            ArrayList<Future<String[]>> results = new ArrayList<>();

            for (File file: files) {
                if (file.isDirectory()) {
                    TokensRemover tokensRemover = new TokensRemover(file, tokenSizeFrom, tokenSizeTo);
                    FutureTask<String[]> task = new FutureTask<>(tokensRemover);
                    results.add(task);
                    new Thread(task).start();
                } else if (searchAndRemove(file)){
                    resultArray.add(file.getName());
                }
            }

            for (Future<String[]> result : results) {
                resultArray.addAll(Arrays.asList(result.get()));
            }

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        String[] strs = new String[resultArray.size()];
        resultArray.toArray(strs);
        return strs;
    }

    boolean searchAndRemove(File file){
        boolean findFlag = false;

        try (Scanner fin = new Scanner(new FileInputStream(file))){
            StringBuilder text = new StringBuilder();

            while (fin.hasNextLine()){
                text.append("\n" + fin.nextLine());
            }

            String tokens[] = Pattern.compile("[ ,!;.\n]").split(text);

            int prevIndex = 0;
            for (String token : tokens) {
                if(token.length() >= tokenSizeFrom && token.length() <= tokenSizeTo) {
                    int index = text.indexOf(token, prevIndex);
                    text.replace(index, index + token.length(), "");
                    prevIndex = index;
                    findFlag = true;
                } else {
                    prevIndex += token.length();
                }
            }

            if (findFlag){
                System.out.println(file.getPath());
                File outFile = new File(file.getPath());
                if (outFile.createNewFile()) {
                    BufferedWriter fout = new BufferedWriter(new FileWriter(outFile));
                    fout.write(text.toString());
                    fout.flush();
                    fout.close();
                }
            }

        } catch (IOException e){
            e.printStackTrace();
        }

        return findFlag;
    }
}
