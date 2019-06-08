package com.example.lenovo.automatedcall;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.inputmethodservice.Keyboard;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private String[] FilePathStrings;
    private String[] FileNameStrings;
    private File[] listFile;
    int phcount = 0;
    File file;
    Button btnUpDirectory, btnSDCard;
    ArrayList<String> pathHistory;
    String lastDirectory;
    int count = 0;
    ArrayList<PhoneNumbers> uploadData;
    ListView lvInternalStorage;
    RelativeLayout rel;
    String ext[] = {".xls", ".xlt", ".xlm", ".xlsx", ".xlsm", ".xltx", ".xltm", ".xlsb", ".xla", ".xlam", ".xll", ".xlw"};
    LinkedList ll = new LinkedList(Arrays.asList(ext));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rel = (RelativeLayout) findViewById(R.id.relLayout);
        lvInternalStorage = (ListView) findViewById(R.id.lvInternalStorage);
        btnUpDirectory = (Button) findViewById(R.id.btnUpDirectory);
        btnSDCard = (Button) findViewById(R.id.btnViewSDCard);
        uploadData = new ArrayList<>();
        checkFilePermissions();
        checkCallPermission();
        lvInternalStorage.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                checkCallPermission();
                if (rel.getVisibility() == View.GONE) {
                    call(uploadData.get(i).getph());
                }
                else {
                    lastDirectory = pathHistory.get(count);
                    int in = lastDirectory.lastIndexOf('.');
                    String name = lastDirectory.substring(lastDirectory.lastIndexOf('/'));
                    if (in != -1 && ll.contains(lastDirectory.substring(in))) {
                        Log.d(TAG, "lvInternalStorage: Selected a file for upload: " + lastDirectory);
                        toastMessage("Selected a file for upload: " + name);
                        readExcelData(lastDirectory);
                        startCalling();
                    } else {
                        count++;
                        pathHistory.add(count, (String) adapterView.getItemAtPosition(i));
                        checkInternalStorage();
                        Log.d(TAG, "lvInternalStorage: " + pathHistory.get(count));
                    }
                }
            }
        });
        //Goes up one directory level
        btnUpDirectory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (count == 0) {
                    Log.d(TAG, "btnUpDirectory: You have reached the highest level directory.");
                } else {
                    pathHistory.remove(count);
                    count--;
                    checkInternalStorage();
                    Log.d(TAG, "btnUpDirectory: " + pathHistory.get(count));
                }
            }
        });

        //Opens the SDCard or phone memory
        btnSDCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                count = 0;
                pathHistory = new ArrayList<String>();
                pathHistory.add(count, System.getenv("EXTERNAL_STORAGE"));
                Log.d(TAG, "btnSDCard: " + pathHistory.get(count));
                checkInternalStorage();
            }
        });

    }

    private void checkCallPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1001);
        }
    }

    private void readExcelData(String filePath) {
        uploadData = new ArrayList<>();
        Log.d(TAG, "readExcelData: Reading Excel File.");

        //decarle input file
        File inputFile = new File(filePath);

        try {
            InputStream inputStream = new FileInputStream(inputFile);
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0);
            int rowsCount = sheet.getPhysicalNumberOfRows();
            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            StringBuilder sb = new StringBuilder();

            //outter loop, loops through rows
            for (int r = 1; r < rowsCount; r++) {
                Row row = sheet.getRow(r);
                int cellsCount = row.getPhysicalNumberOfCells();
                //inner loop, loops through columns
                for (int c = 0; c < cellsCount; c++) {
                    //handles if there are to many columns on the excel sheet.
                    if (c > 1) {
                        Log.e(TAG, "readExcelData: ERROR. Excel File Format is incorrect! ");
                        toastMessage("ERROR: Excel File Format is incorrect!");
                        break;
                    } else {
                        String value = getCellAsString(row, c, formulaEvaluator);
                        String cellInfo = "r:" + r + "; c:" + c + "; v:" + value;
                        value = rectify(value);
                        Log.d(TAG, "readExcelData: Data from row: " + cellInfo);
                        sb.append(value);
                    }
                }
                sb.append(":");
            }
            Log.d(TAG, "readExcelData: STRINGBUILDER: " + sb.toString());

            parseStringBuilder(sb);

        } catch (FileNotFoundException e) {
            Log.e(TAG, "readExcelData: FileNotFoundException. " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "readExcelData: Error reading inputstream. " + e.getMessage());
        }
    }

    private String rectify(String value) {
        String res = "";
        int len = value.length();
        if (len <= 10)
            return value;
        int dot = value.indexOf('.');
        int E = value.indexOf('E');
        res = value.substring(0, dot) + value.substring(dot, E);
        return res;
    }

    /**
     * Method for parsing imported data and storing in ArrayList<XYValue>
     */
    public void parseStringBuilder(StringBuilder mStringBuilder) {
        Log.d(TAG, "parseStringBuilder: Started parsing.");

        // splits the sb into rows.
        String[] rows = mStringBuilder.toString().split(":");

        //Add to the ArrayList<XYValue> row by row
        for (int i = 0; i < rows.length; i++) {
            //Split the columns of the rows
            String columns = rows[i];

            //use try catch to make sure there are no "" that try to parse into doubles.
            try {
                String x = columns;

                String cellInfo = x;
                Log.d(TAG, "ParseStringBuilder: Data from row: " + cellInfo);

                //add the the uploadData ArrayList
                uploadData.add(new PhoneNumbers(x));

            } catch (NumberFormatException e) {

                Log.e(TAG, "parseStringBuilder: NumberFormatException: " + e.getMessage());

            }
        }

        printDataToLog();
    }

    private void printDataToLog() {
        Log.d(TAG, "printDataToLog: Printing data to log...");

        for (int i = 0; i < uploadData.size(); i++) {
            String x = uploadData.get(i).getph();
            Log.d(TAG, "printDataToLog: (x,y): " + x);
        }
    }

    String getCellAsString(Row row, int c, FormulaEvaluator formulaEvaluator) {
        String value = "";
        try {
            Cell cell = row.getCell(c);
            CellValue cellValue = formulaEvaluator.evaluate(cell);
            switch (cellValue.getCellType()) {
                case Cell.CELL_TYPE_BOOLEAN:
                    value = "" + cellValue.getBooleanValue();
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    double numericValue = cellValue.getNumberValue();
                    if (HSSFDateUtil.isCellDateFormatted(cell)) {
                        double date = cellValue.getNumberValue();
                        SimpleDateFormat formatter =
                                new SimpleDateFormat("MM/dd/yy");
                        value = formatter.format(HSSFDateUtil.getJavaDate(date));
                    } else {
                        value = "" + numericValue;
                    }
                    break;
                case Cell.CELL_TYPE_STRING:
                    value = "" + cellValue.getStringValue();
                    break;
                default:
            }
        } catch (NullPointerException e) {

            Log.e(TAG, "getCellAsString: NullPointerException: " + e.getMessage());
        }
        return value;
    }

    private void checkInternalStorage() {
        Log.d(TAG, "checkInternalStorage: Started.");
        try {
            if (!Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                toastMessage("No SD card found...");
            } else {
                // Locate the image folder in your SD Car;d
                file = new File(pathHistory.get(count));
                Log.d(TAG, "checkInternalStorage: directory path: " + pathHistory.get(count));
            }

            listFile = file.listFiles();

            // Create a String array for FilePathStrings
            FilePathStrings = new String[listFile.length];

            // Create a String array for FileNameStrings
            FileNameStrings = new String[listFile.length];

            for (int i = 0; i < listFile.length; i++) {
                // Get the path of the image file
                FilePathStrings[i] = listFile[i].getAbsolutePath();
                // Get the name image file
                FileNameStrings[i] = listFile[i].getName();
            }

            for (int i = 0; i < listFile.length; i++) {
                Log.d("Files", "FileName:" + listFile[i].getName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, FilePathStrings);
            lvInternalStorage.setAdapter(adapter);

        } catch (NullPointerException e) {
            Log.e(TAG, "checkInternalStorage: NULLPOINTEREXCEPTION " + e.getMessage());
        }
    }

    private void checkFilePermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.READ_EXTERNAL_STORAGE");
            permissionCheck += this.checkSelfPermission("Manifest.permission.WRITE_EXTERNAL_STORAGE");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,}, 1001); //Any number
            }
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1001);
            }

        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void startCalling() {
        ArrayList<String> numbers = new ArrayList<String>();
        rel.setVisibility(View.GONE);
        for (int i1 = 0; i1 < uploadData.size(); i1++) {
            numbers.add(uploadData.get(i1).getph());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, numbers);
        lvInternalStorage.setAdapter(adapter);
        delay(2);
        call(uploadData.get(phcount).getph());
    }
    private void call(String number){
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:+91" + number));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1001);
        }
        startActivity(callIntent);
        new CountDownTimer(35000,1000){
            @Override
            public void onTick(long l) {
                String time = l/1000 + "";
                Log.i("Time passed:" , time);
                if(Integer.parseInt(time) <= 10)
                    Toast.makeText(MainActivity.this, time + "secs for next call", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                phcount++;
                if(phcount<uploadData.size())
                    call(uploadData.get(phcount).getph());
                else {
                    toastMessage("ALL CALLS COMPLETE");
                    phcount = 0;
                }
            }

            @Override
            protected void finalize() throws Throwable {
                super.finalize();
            }
        }.start();
    }
    private void delay(int secs){
        new CountDownTimer(secs*1000,1000){
            @Override
            public void onTick(long l) {
                String time = l/1000 + "";
                Log.i("Time passed:" , time);

            }

            @Override
            public void onFinish() {
                Log.i("Time over:", "Count Down stops");
            }

            @Override
            protected void finalize() throws Throwable {
                super.finalize();
            }
        }.start();
    }
}
