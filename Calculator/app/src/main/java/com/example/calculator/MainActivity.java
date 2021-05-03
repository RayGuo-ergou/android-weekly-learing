package com.example.calculator;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    EditText operand1EditText;
    EditText operand2EditText;
    TextView resultTextView;
    // ADD FUNCTION
    public void addClick(View view)
    {
        Double result = Double.parseDouble(operand1EditText.getText().toString())  + Double.parseDouble(operand2EditText.getText().toString()) ;
        resultTextView.setText(result.toString());
    }
    //SUBTRACT FUNCTION
    public void subClick(View view)
    {
        Double result = Double.parseDouble(operand1EditText.getText().toString()) - Double.parseDouble(operand2EditText.getText().toString()) ;
        resultTextView.setText(result.toString());
    }
    //DIVISION FUNCTION
    public void divClick(View view)
    {
        Double result = Double.parseDouble(operand1EditText.getText().toString()) / Double.parseDouble(operand2EditText.getText().toString()) ;
        resultTextView.setText(result.toString());
    }
    //MULTIPLICATION FUNCTION
    public void mulClick(View view)
    {
        Double result = Double.parseDouble(operand1EditText.getText().toString()) * Double.parseDouble(operand2EditText.getText().toString()) ;
        resultTextView.setText(result.toString());
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        operand1EditText = findViewById(R.id.operand1EditText);
        operand2EditText = findViewById(R.id.operand2EditText);
        resultTextView = findViewById(R.id.resultTextView);

    }


}
