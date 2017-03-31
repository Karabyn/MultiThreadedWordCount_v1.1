/**
 * Created by petro on 31-Mar-17.
 */
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by petro on 23-Mar-17.
 */

public class WordCounter {

    private static StringBuilder sb = new StringBuilder();
    private static final File runconfig = new File("runconfig.txt");
    private static File filename; //= new File("src/books1-14.txt"); file to read text from
    private static int numberOfThreads;
    private static String[] textBlocks;
    private static HashMap<String, Integer> mainHashMap = new HashMap<>();
    private static long readingTime = 0;
    private static long wordCountTime = 0;
    private static long totalTime = 0;

    public static void main(String[] args) {
        WordCounter wordCounter = new WordCounter();
        wordCounter.getRunConfig();
        textBlocks = new String[numberOfThreads];

        // reading
        long readingStartTime = System.nanoTime();
        wordCounter.readFile(filename);
        long executionTime = System.nanoTime() - readingStartTime;
        System.out.println("Reading time: " + String.format("%d s %d ms", TimeUnit.NANOSECONDS.toSeconds(executionTime),
                TimeUnit.NANOSECONDS.toMillis(executionTime) - TimeUnit.SECONDS.toMillis(TimeUnit.NANOSECONDS.toSeconds(executionTime))));

        // extracting and counting words in threads
        long wordCountStartTime = System.nanoTime();
        wordCounter.divideString();

        CounterThread[] counterThreads = new CounterThread[numberOfThreads];
        for(int i = 0; i < numberOfThreads; i++) {
            counterThreads[i] = new CounterThread(textBlocks[i]);
            counterThreads[i].start();
        }
        for(CounterThread thread : counterThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (Map.Entry<String, Integer> e : thread.wordCountHashMap.entrySet())
                mainHashMap.merge(e.getKey(), e.getValue(), Integer::sum);
        }
        executionTime = System.nanoTime() - wordCountStartTime;
        System.out.println("Word count time: " + String.format("%d s %d ms", TimeUnit.NANOSECONDS.toSeconds(executionTime),
                TimeUnit.NANOSECONDS.toMillis(executionTime) - TimeUnit.SECONDS.toMillis(TimeUnit.NANOSECONDS.toSeconds(executionTime))));

        wordCounter.sortByOccurrences(mainHashMap);
        wordCounter.sortByAlphabet(mainHashMap);
    }

    private void getRunConfig() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(runconfig));
            String input = bufferedReader.readLine();
            numberOfThreads = Integer.valueOf(input.substring(0, input.indexOf(" ")));
            input = bufferedReader.readLine();
            filename = new File(input.substring(0, input.indexOf(" ")));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readFile(File filename) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(filename));
            String line = bufferedReader.readLine();
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedReader.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Divides the input text into equal parts according
     * to the number of threads for further processing.
     */
    private void divideString() {
        String text = sb.toString();
        int textLength = text.length();
        double n = numberOfThreads;
        for(int i = 0; i < numberOfThreads; i ++) {
            textBlocks[i] = text.substring((int)(textLength * (i / n)), (int)(textLength * ((i + 1) / n)));
        }

    }

    private void sortByOccurrences(HashMap wordCountHashMap) {
        List list = new ArrayList(wordCountHashMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue() - a.getValue();
            }
        });
        writeToFile(list, "wordsByOccurrencesOutput.txt");
        System.out.println("wordsByOccurrences: " + list.subList(0, 100));
    }

    private void sortByAlphabet(HashMap wordCountHashMap) {
        List list = new ArrayList(wordCountHashMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return a.getKey().compareTo(b.getKey());
            }
        });
        writeToFile(list, "wordsByAlphabet.txt");
        //System.out.println("wordsByAlphabet: " + list.subList(0, 100));
    }

    private void writeToFile(List list, String filename) {
        try{
            PrintWriter writer = new PrintWriter(filename, "UTF-8");
            for(Object entry : list) {
                HashMap.Entry e = (HashMap.Entry) entry;
                writer.println(e.getKey() + "=" + e.getValue());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int numberOfDistinctWords(HashMap<String, Integer> wordCount) {
        return wordCount.size();
    }

}
class CounterThread extends Thread {

    private String text;
    public HashMap<String,Integer> wordCountHashMap = new HashMap<>();

    public CounterThread(String text) {
        this.text=text;
    }

    private String[] extractOnlyWords() {
        return text.replaceAll("[\\W]", " ").toLowerCase().split("\\s++");
    }

    private void wordCount(String[] words) {
        for(String word : words) {
            if(!wordCountHashMap.containsKey(word)){
                wordCountHashMap.put(word, 1);
            }
            else {
                wordCountHashMap.put(word, wordCountHashMap.get(word) + 1);
            }
        }
    }

    @Override
    public void run() {
        String[] block = extractOnlyWords();
        wordCount(block);
    }
}
