package com.mti.cryosite.ad230flagquizgame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity implements View.OnClickListener {
    //String used when logging error messages
    private static final String TAG = "FlagQuizGame Activity";

    private List<String> fileNameList; //flag file names
    private List<String> quizCountriesList; //names of countries in quiz
    private Map<String, Boolean> regionsMap; //which regions are enabled
    private String correctAnswer; //correct country for the current flag
    private int totalGuesses; //number of guesses made
    private int correctAnswers; //number of correct guesses
    private int guessRows; //number of rows displaying choices
    private Random random; //random number generator
    private Handler handler; //used to delay loading next flag
    private Animation shakeAnimation; //animation for incorrect guess

    private TextView answerTextView; //displays Correct! or Incorrect!
    private TextView questionNumberTextView; //shows current question #
    private ImageView flagImageView; //displays a flag
    private TableLayout buttonTableLayout; //table of answer Buttons

    //called when the activity is first created
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); //call the superclass's method
        setContentView(R.layout.activity_main); //inflate the GUI

        fileNameList = new ArrayList<String>(); //list of image file names
        quizCountriesList = new ArrayList<String>(); //flags in this quiz
        regionsMap = new HashMap<String, Boolean>(); //HashMap of regions
        guessRows = 1; //default to one row of choices
        random = new Random(); //initialize the random number generator
        handler = new Handler(); //used to perform delayed operations

        //load the shake animation that's used for incorrect answers
        shakeAnimation =
                AnimationUtils.loadAnimation(this, R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3); //animation repeats 3 times

        //get array of world regions from strings.xml
        String[] regionNames =
                getResources().getStringArray(R.array.regionsList);

        //by default, countries are chosen from all regions
        for (String region : regionNames )
            regionsMap.put(region, true);

        //get references to GUI components
        questionNumberTextView = (TextView) findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) findViewById(R.id.flagImageView);
        buttonTableLayout = (TableLayout) findViewById(R.id.buttonTableLayout);
        answerTextView = (TextView) findViewById(R.id.answerTextView);

        //set questionNumberTextView's text
        questionNumberTextView.setText(
                getResources().getString(R.string.question) +
                " 1 " +
                getResources().getString(R.string.of) +
                " 10");

        resetQuiz(); //start a new quiz
    }//onCreate(Bundle savedInstanceState)

    //set up and start the next quiz
    private void resetQuiz() {
        //use the AssetManager to get the image flag
        //file names for only the enabled regions
        AssetManager assets = getAssets(); //get the app's AssetManager
        fileNameList.clear(); //empty the list

        try {
            Set<String> regions = regionsMap.keySet(); //get Set of regions

            //loop through each region
            for (String region : regions) {
                //if region is enabled
                if (regionsMap.get(region)) {
                    //get a list of all flag image files in this region
                    String[] paths = assets.list(region);

                    for (String path : paths)
                        fileNameList.add(path.replace(".png", ""));
                }//endif - (regionsMap.get(region))
            }//endfor - (String region : regions)
        }//endtry
        catch (IOException e) {
            Log.e(TAG, "Error loading image file names", e);
        }//endcatch - (IOException e)

        correctAnswers = 0; //reset the number of correct answers made
        totalGuesses = 0; //reset the total number of guesses the user made
        quizCountriesList.clear(); //clear prior list of quiz countries

        //add 10 random file names to the quizCountriesList
        int flagCounter = 1;
        int numberOfFlags = fileNameList.size(); //get number of flags

        while (flagCounter <= 10) {
            int randomIndex = random.nextInt(numberOfFlags); //random index

            //get the random file name
            String fileName = fileNameList.get(randomIndex);

            //if the region is enabled and it hasn't already been chosen
            if (!quizCountriesList.contains(fileName)) {
                quizCountriesList.add(fileName); //add the file to the list
                ++flagCounter;
            }//endif - (!quizCountriesList.contains(fileName))
        }//endwhile - (flagCounter <= 10)

        loadNextFlag(); //start the quiz by loading the first flag
    }//resetQuiz()

    //after the user guesses a correct flag, load the next flag
    private void loadNextFlag() {
        //get file name of the next flag and remove it from the list
        String nextImageName = quizCountriesList.remove(0);
        correctAnswer = nextImageName; //update the correct answer

        answerTextView.setText(""); //clear answerTextView

        //display the number of the current question in the quiz
        questionNumberTextView.setText(
                getResources().getString(R.string.question) + " " +
                        (correctAnswers + 1) + " " +
                        getResources().getString(R.string.of) + " 10");

        //extract the region from the next image's name
        String region =
                nextImageName.substring(0, nextImageName.indexOf('-'));

        //use AssetManager to load next image from assets folder
        AssetManager assets = getAssets(); //get app's AssetManager
        InputStream stream; //used to read in flag images

        try {
            //get an InputStream to the asset representing the next flag
            stream = assets.open(
                    region +
                    "/" +
                    nextImageName +
                    ".png");

            //load the asset as a Drawable and display on the flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImageName);
            flagImageView.setImageDrawable(flag);
        }//endtry
        catch (IOException e) {
            Log.e(TAG, "Error loading " + nextImageName, e);
        }//endcatch - (IOException e)

        //clear prior answer Buttons from TableRows
        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row)
            ((TableRow) buttonTableLayout.getChildAt(row)).removeAllViews();

        Collections.shuffle(fileNameList); //shuffle file names

        //put the correct answer at the end of fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        //get a reference to the LayoutInflater service
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        //add 3, 6, or 9 answer Buttons based on the value of guessRows
        for (int row = 0; row < guessRows; row++) {
            TableRow currentTableRow = getTableRow(row);

            //place Buttons in currentTableRow
            for (int column = 0; column < 3; column++) {
                //inflate guess_button.xml to create new Button
                Button newGuessButton =
                        (Button) inflater.inflate(R.layout.guess_button, null);

                //get country name and set it as newGuessButton's text
                String fileName = fileNameList.get((row * 3) + column);
                newGuessButton.setText(getCountryName(fileName));

                //register answerButtonListener to respond to button clicks
                newGuessButton.setOnClickListener(this);
                currentTableRow.addView(newGuessButton);
            }//endfor - (int column = 0; column < 3; column++)
        }//endfor - (int row = 0; row < guessRows; row++)

        //randomly replace one Button with the correct answer
        int row = random.nextInt(guessRows); //pick random row
        int column = random.nextInt(3); //pick random column
        TableRow randomTableRow = getTableRow(row); //get the TableRow
        String countryName = getCountryName(correctAnswer);
        ((Button)randomTableRow.getChildAt(column)).setText(countryName);
    }//loadNextFlag()

    //returns the specified TableRow
    private TableRow getTableRow(int row) {
        return (TableRow) buttonTableLayout.getChildAt(row);
    }//getTableRow(int row)

    //parses the country flag file name and returns the country name
    private String getCountryName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }//getCountryName(String name)

    //called when the user selects an answer
    private void submitGuess(Button guessButton) {
        String guess = guessButton.getText().toString();
        String answer = getCountryName(correctAnswer);
        ++totalGuesses; //increment the number of guesses the user has made

        //if the guess is correct
        if (guess.equals(answer)) {
            ++correctAnswers; //increment the number of correct answers

            //display "Correct!" in green text
            answerTextView.setText(answer + "!");
            answerTextView.setTextColor(
                    getResources().getColor(R.color.correct_answer));

            disableButtons(); //disable all answer Buttons

            //if the user has correctly identified 10 flags
            if (correctAnswers == 10) {
                //create a new AlertDialog Builder
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.reset_quiz); //title bar string

                //set the AlertDialog's message to display game results
                builder.setMessage(String.format("%d %s, %.02f%% %s",
                        totalGuesses, getResources().getString(R.string.guesses),
                        (1000 / (double) totalGuesses),
                        getResources().getString(R.string.correct)));

                builder.setCancelable(false);

                //add "Reset Quiz" Button
                builder.setPositiveButton(R.string.reset_quiz,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                resetQuiz();
                            }//onClick(DialogInterface dialog, int id)
                        }//end anonymous inner class
                ); //end call to setPositiveButton

                //create AlertDialog from the Builder
                AlertDialog resetDialog = builder.create();
                resetDialog.show(); //display the Dialog
            }//endif - (correctAnswers == 10)
            //answer is correct but quiz is not over
            else {
                //load the next flag after a 1-second delay
                handler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                loadNextFlag();
                            }//run()
                        }, 1000); //1000 milliseconds for 1-second delay
            }//endelse - (correctAnswers == 10)
        }//endif - (guess.equals(answer))
        //guess was incorrect
        else {
            //play the animation
            flagImageView.startAnimation(shakeAnimation);

            //display "Incorrect!" in red
            answerTextView.setText(R.string.incorrect_answer);
            answerTextView.setTextColor(
                    getResources().getColor(R.color.incorrect_answer));
            guessButton.setEnabled(false); //disable the incorrect answer
        }//endelse - (guess.equals(answer))
    }//submitGuess(Button guessButton)

    //utility method that disables all answer Buttons
    private void disableButtons() {
        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row) {
            TableRow tableRow = (TableRow) buttonTableLayout.getChildAt(row);
            for (int i = 0; i < tableRow.getChildCount(); ++i)
                tableRow.getChildAt(i).setEnabled(false);
        }//endfor -(int row = 0; row < buttonTableLayout.getChildCount(); ++row)
    }//disableButtons()

    //create constants for each menu id
    private final int CHOICES_MENU_ID = Menu.FIRST;
    private final int REGIONS_MENU_ID = Menu.FIRST + 1;

    //called when the user accesses the options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        //add two options to the menu - "Choices" and "Regions"
        menu.add(Menu.NONE, CHOICES_MENU_ID, Menu.NONE, R.string.choices);
        menu.add(Menu.NONE, REGIONS_MENU_ID, Menu.NONE, R.string.regions);

        return true; //display the menu
    }//onCreateOptionsMenu(Menu menu)

    //called when the user selects an option from the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //switch the menu id of the user-selected option
        switch (item.getItemId()) {
            case CHOICES_MENU_ID:
                //create a list of the possible numbers of answer choices
                final String[] possibleChoices =
                        getResources().getStringArray(R.array.guessesList);

                //create a new AlertDialog Builder and set its title
                AlertDialog.Builder choicesBuilder =
                        new AlertDialog.Builder(this);
                choicesBuilder.setTitle(R.string.choices);

                //add possibleChoices's items to the Dialog and set the
                //behavior when one of the items is clicked
                choicesBuilder.setItems(R.array.guessesList,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                //update guessRows to match the user's choice
                                guessRows = Integer.parseInt(
                                        possibleChoices[item].toString()) / 3;
                                resetQuiz(); //reset the quiz
                            }//onClick(DialogInterface dialog, int item)
                        }//end anonymous inner class
                );  //end call to setItems

                //create an AlertDialog from the Builder
                AlertDialog choicesDialog = choicesBuilder.create();
                choicesDialog.show(); //show the Dialog
                return true;

            case REGIONS_MENU_ID:
                //get array of world regions
                final String[] regionNames =
                        regionsMap.keySet().toArray(new String[regionsMap.size()]);

                //boolean array representing whether each region is enabled
                boolean[] regionsEnabled = new boolean[regionsMap.size()];
                for (int i = 0; i < regionsEnabled.length; ++i)
                    regionsEnabled[i] = regionsMap.get(regionNames[i]);

                //create an AlertDialog Builder and set the dialog's title
                AlertDialog.Builder regionsBuilder =
                        new AlertDialog.Builder(this);
                regionsBuilder.setTitle(R.string.regions);

                //replace _ with space in region names for display purposes
                String[] displayNames = new String[regionNames.length];
                for (int i = 0; i < regionNames.length; ++i)
                    displayNames[i] = regionNames[i].replace('_', ' ');

                //add displayNames to the Dialog and set the behavior
                //when one of the items is clicked
                regionsBuilder.setMultiChoiceItems(
                        displayNames, regionsEnabled,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                //include or exclude the clicked region
                                //depending on whether or not it's checked
                                regionsMap.put(regionNames[which].toString(), isChecked);
                            }//onClick(DialogInterface dialog, int which, boolean isChecked)
                        }//end anonymous inner class
                ); //end call to setMultiChoiceItems

                //resets quiz when user presses the "Reset Quiz" Button
                regionsBuilder.setPositiveButton(R.string.reset_quiz,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int button) {
                                resetQuiz(); //reset the quiz
                            }//onClick(DialogInterface dialog, int button)
                        }//end anonymous inner class
                ); //end call to method setPositiveButton

                //create a dialog from the Builder
                AlertDialog regionsDialog = regionsBuilder.create();
                regionsDialog.show(); //display the Dialog
                return true;
        }//endswitch - (item.getItemId())

        return super.onOptionsItemSelected(item);
    }//onOptionsItemSelected(MenuItem item)

    @Override
    public void onClick(View v) {
        submitGuess((Button) v); //pass selected Button to submitGuess
    }//onClick(View v)
}//end FlagQuizGame

/*
Modified from source provided by Deitel & Associates, Inc. and Pearson Education, Inc.
 */