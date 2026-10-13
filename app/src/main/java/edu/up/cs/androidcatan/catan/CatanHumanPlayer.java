package edu.up.cs.androidcatan.catan;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.support.constraint.Group;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.up.cs.androidcatan.R;
import edu.up.cs.androidcatan.catan.actions.CatanBuildCityAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuildRoadAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuildSettlementAction;
import edu.up.cs.androidcatan.catan.actions.CatanBuyDevCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanEndTurnAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberDiscardAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberMoveAction;
import edu.up.cs.androidcatan.catan.actions.CatanRobberStealAction;
import edu.up.cs.androidcatan.catan.actions.CatanRollDiceAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithBankAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithCustomPortAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithPortAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseKnightCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseMonopolyCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseRoadBuildingCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseVictoryPointCardAction;
import edu.up.cs.androidcatan.catan.actions.CatanUseYearOfPlentyCardAction;
import edu.up.cs.androidcatan.catan.gamestate.Board;
import edu.up.cs.androidcatan.catan.gamestate.DevelopmentCard;
import edu.up.cs.androidcatan.catan.gamestate.Hexagon;
import edu.up.cs.androidcatan.catan.gamestate.Port;
import edu.up.cs.androidcatan.catan.gamestate.buildings.City;
import edu.up.cs.androidcatan.catan.gamestate.buildings.Road;
import edu.up.cs.androidcatan.catan.graphics.BoardSurfaceView;
import edu.up.cs.androidcatan.catan.graphics.HexagonDrawable;
import edu.up.cs.androidcatan.catan.graphics.HexagonGrid;
import edu.up.cs.androidcatan.game.GameHumanPlayer;
import edu.up.cs.androidcatan.game.GameMainActivity;
import edu.up.cs.androidcatan.game.actionMsg.GameOverAckAction;
import edu.up.cs.androidcatan.game.infoMsg.GameInfo;
import edu.up.cs.androidcatan.game.infoMsg.IllegalMoveInfo;
import edu.up.cs.androidcatan.game.infoMsg.NotYourTurnInfo;

/**
 * @author Alex Weininger
 * @author Andrew Lang
 * @author Daniel Borg
 * @author Niraj Mali
 * @version December 5th, 2018
 * https://github.com/alexweininger/android-catan
 **/
public class CatanHumanPlayer extends GameHumanPlayer implements OnClickListener {
    private final String TAG = "CatanHumanPlayer"; // log tag

    // instance variables for logic
    private ArrayList<Integer> buildingsBuiltOnThisTurn = new ArrayList<>();
    private int intersectionOfSettlementSetupTurn;
    private float lastTouchDownXY[] = new float[2];
    private boolean debugMode = false; // debug mode
    private boolean isMenuOpen = false; // if a menu is currently open

    private boolean readyToDraw = false; // is the game ready to draw

    private int selectedHexagonId = -1; // currently selected hexagon id
    private ArrayList<Integer> selectedIntersections = new ArrayList<>(); // currently selected intersections

    // resourceCard index values: 0 = Brick, 1 = Lumber, 2 = Grain, 3 = Ore, 4 = Wool
    private int[] robberDiscardedResources = new int[]{0, 0, 0, 0, 0};  //How many resources the player would like to discard
    private int selectedDevCard = -1;
    private int selectedResourceId = -1;
    private int firstYearOfPlentyResource = -1;
    private TextView messageTextView = (TextView) null;

    private ArrayList<String> devCards = new ArrayList<>();

    /* ------------- Building Buttons -------------------- */
    private Button buildCityButton = null;
    private Button buildRoadButton = null;
    private Button buildSettlementButton = null;

    /* ------------- Action Buttons -------------------- */
    private Button sidebarOpenDevCardMenuButton = null;
    private Button tradeButton = null;
    private Button useDevCard = null;
    private Spinner devCardList = null;

    /* ------ Turn Buttons ------- */
    private Button rollButton = null;
    private ImageView diceImageLeft = null;
    private ImageView diceImageRight = null;
    private Button endTurnButton = null;

    /* ------------- Misc Buttons -------------------- */

    private Button sidebarMenuButton = (Button) null;
    private ImageView buildingCosts = null;
    private Button sidebarScoreboardButton = (Button) null;

    /*------------ Help Menu Buttons and Groups ----------------- */
    private Button winningHelpButton = (Button) null;
    private Button setUpPhaseHelpButton = (Button) null;
    private Button buildingHelpButtonm = (Button) null;
    private Button developmentCardHelpButon = (Button) null;
    private Button tradingHelpButton = (Button) null;
    private Button robberHelpButton = (Button) null;

    private Button winningHelpBackButton = (Button) null;
    private Button setUpPhaseHelpBackButton = (Button) null;
    private Button buildingHelpBackButton = (Button) null;
    private Button developmentCardHelpBackButton = (Button) null;
    private Button tradingHelpBackButton = (Button) null;
    private Button robberHelpBackButton = (Button) null;

    private Group helpMenu = (Group) null;
    private Group winningHelpMenu = (Group) null;
    private Group setUpPhaseHelpMenu = (Group) null;
    private Group buildingHelpMenu = (Group) null;
    private Group developmentCardHelpMenu = (Group) null;
    private Group tradingHelpMenu = (Group) null;
    private Group robberHelpMenu = (Group) null;


    /* ------------- resource count text views -------------------- */
    private TextView[] resourceValues;
    private TextView[] playerScores;
    private TextView[] playerNameTextViews;

    // misc sidebar TextViews
    private TextView myScore = (TextView) null;
    private TextView currentTurnIdTextView = (TextView) null;
    private TextView playerNameSidebar = (TextView) null;

    /*-------------------- Robber Buttons ---------------------------*/
    private ImageView[] robberPlusImages;
    private ImageView[] robberMinusImages;
    private TextView[] robberDiscardResourceValues;

    private ImageView robberBrickPlus = (ImageView) null;
    private ImageView robberBrickMinus = (ImageView) null;
    private ImageView robberLumberPlus = (ImageView) null;
    private ImageView robberLumberMinus = (ImageView) null;
    private ImageView robberGrainPlus = (ImageView) null;
    private ImageView robberGrainMinus = (ImageView) null;
    private ImageView robberOrePlus = (ImageView) null;
    private ImageView robberOreMinus = (ImageView) null;
    private ImageView robberWoolPlus = (ImageView) null;
    private ImageView robberWoolMinus = (ImageView) null;


    private TextView robberBrickAmount = (TextView) null;
    private TextView robberLumberAmount = (TextView) null;
    private TextView robberGrainAmount = (TextView) null;
    private TextView robberOreAmount = (TextView) null;
    private TextView robberWoolAmount = (TextView) null;

    private TextView robberDiscardMessage = (TextView) null;
    private Button robberConfirmDiscard = (Button) null;
    private TextView robberHexMessage = (TextView) null;

    /*---------------------Trade Items------------------------*/
    private ImageView[] tradeReceiveSelectionBoxes;
    private ImageView[] tradeGiveSelectionBoxes;

    private ImageView[] tradeReceiveImages;
    private ImageView[] tradeGiveImages;

    //Trade Buttons - Receive
    private ImageView brickSelectionBoxReceive = (ImageView) null;
    private ImageView grainSelectionBoxReceive = (ImageView) null;
    private ImageView lumberSelectionBoxReceive = (ImageView) null;
    private ImageView oreSelectionBoxReceive = (ImageView) null;
    private ImageView woolSelectionBoxReceive = (ImageView) null;

    //Trade Buttons - Give
    private ImageView brickSelectionBoxGive = (ImageView) null;
    private ImageView grainSelectionBoxGive = (ImageView) null;
    private ImageView lumberSelectionBoxGive = (ImageView) null;
    private ImageView oreSelectionBoxGive = (ImageView) null;
    private ImageView woolSelectionBoxGive = (ImageView) null;

    //Trade Menu - Receive
    private ImageView image_trade_menu_rec_brick = (ImageView) null;
    private ImageView image_trade_menu_rec_grain = (ImageView) null;
    private ImageView image_trade_menu_rec_ore = (ImageView) null;
    private ImageView image_trade_menu_rec_lumber = (ImageView) null;
    private ImageView image_trade_menu_rec_wool = (ImageView) null;

    //Trade Menu - Give
    private ImageView image_trade_menu_give_brick = (ImageView) null;
    private ImageView image_trade_menu_give_grain = (ImageView) null;
    private ImageView image_trade_menu_give_lumber = (ImageView) null;
    private ImageView image_trade_menu_give_ore = (ImageView) null;
    private ImageView image_trade_menu_give_wool = (ImageView) null;

    //Trade Menu - Confirm and Cancel
    private Button button_trade_menu_confirm = (Button) null;
    private Button button_trade_menu_cancel = (Button) null;
    private int tradeGiveSelection = -1;
    private int tradeReceiveSelection = -1;

    private TextView tradingWithBankOrPort = (TextView) null;

    /*------------Monopoly Menu - Resource Icons---------------------*/

    //Monopoly Menu - Resource Icons
    private ImageView[] pickResourceIcons;

    private ImageView monopolyBrickIcon = (ImageView) null;
    private ImageView monopolyGrainIcon = (ImageView) null;
    private ImageView monopolyLumberIcon = (ImageView) null;
    private ImageView monopolyOreIcon = (ImageView) null;
    private ImageView monopolyWoolIcon = (ImageView) null;

    //Monopoly Menu - SelectionBoxes

    private ImageView[] pickResourceSelectionBoxes;

    private ImageView monopolyBrickSelectionBox = (ImageView) null;
    private ImageView monopolyGrainSelectionBox = (ImageView) null;
    private ImageView monopolyLumberSelectionBox = (ImageView) null;
    private ImageView monopolyOreSelectionBox = (ImageView) null;
    private ImageView monopolyWoolSelectionBox = (ImageView) null;

    //Monopoly Menu - Confirm
    private TextView monopolyConfirm = (TextView) null;

    //Dev Card Menu
    private TextView devcard_text_name = (TextView) null;
    private TextView devcard_text_info = (TextView) null;

    //Other Groups
    private Group scoreBoardGroup = (Group) null;
    private Group developmentGroup = (Group) null;
    private Group tradeGroup = (Group) null;
    private Group robberDiscardGroup = (Group) null;
    private Group robberChooseHexGroup = (Group) null;
    private Group pickResourceGroup = (Group) null;

    //Music
    /**
     * External Citation
     * Date: 3 December 2018
     * Problem: Needed to be able to play music files
     * Resource: https://www.androidhive.info/2012/03/android-building-audio-player-tutorial/
     * Solution:  I used parts of the example code provided.
     * Code Line: 264
     */

    private MediaPlayer menuMusic = new MediaPlayer();
    private MediaPlayer generalMusic = new MediaPlayer();
    private Boolean newGame = true;

    /* ------------------------------ Scoreboard trophy images ------------------------------------ */

    private ImageView roadTrophyImages[] = new ImageView[4];
    private ImageView armyTrophyImages[] = new ImageView[4];

    private GameMainActivity myActivity;  // the android activity that we are running
    public CatanGameState state = null; // game state
    private BoardSurfaceView boardSurfaceView;
    private TextView hudLastRoll, hudHandFirst, hudHandSecond, hudCards, hudLatestAction;
    private static final String[] HUD_CARD_NAMES =
            {"Knight", "Victory Point", "Year of Plenty", "Monopoly", "Road Building"};
    private TextView diceStatusChip;
    private TextView lastActionChip;
    private TextView handStatusChip;
    private final ArrayList<String> activityLog = new ArrayList<>();
    private int[] lastOwnResources;
    private static final String[] HAND_RESOURCES = {"Brick", "Grain", "Lumber", "Ore", "Wool"};
    private volatile boolean initialStateDisplayed;

    /** True only once the initial game state has populated the board display. */
    public boolean isInitialStateDisplayed() {
        return initialStateDisplayed && boardSurfaceView != null && state != null;
    }
    private boolean controlsCollapsed = false;


    /*--------------------- Constructors ------------------------*/

    /**
     * constructor for the CatanHumanPlayer
     *
     * @param name the name of the player
     */
    public CatanHumanPlayer(String name) {
        super(name);
    }

    /*---------------------------------- onClick Method -----------------------------------------*/

    /**
     * this method gets called when the user clicks ANY button that has the listener set to 'this'
     *
     * @param button the button that was clicked
     */
    public void onClick(View button) {
        Log.d(TAG, "onClick() called with: button = [" + button + "]");

        if (this.state == null) {
            Log.e(TAG, "onClick: state is null.");
            return;
        } // check if state is null

        messageTextView.setTextColor(Color.WHITE);

        /**
         External Citation
         Date: 5 December 2018
         Problem: Needed to disable the navigation button bar and notification bar; enable fullscreen mode
         Resource: https://developer.android.com/training/system-ui/immersive

         Solution: Remove the visibility of various UI features using the code below
         */
        View decorView = myActivity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);


        /* ---------------------------- Building Sidebar Button OnClick() Handlers --------------------- */

        // Road button on the sidebar.
        if (button.getId() == R.id.sidebar_button_road) {
            Log.i(TAG, "onClick: " + state.getBoard().toString() + "");

            //Player does not have enough resources to build the road
            if (!state.isSetupPhase() && state.getFreeRoadsRemaining() == 0
                    && !state.getPlayerList().get(state.getCurrentPlayerId()).hasResourceBundle(Road.resourceCost)) {
                messageTextView.setText(R.string.not_enough_for_road);
                shake(messageTextView);
                return;
            }

            //Player has not selected enough intersections build a road (2 needed)
            if (selectedIntersections.size() != 2) {
                messageTextView.setText(R.string.need_2_ints_for_road);
                /**
                 External Citation
                 Date: 2 November 2018
                 Problem: Needed a way to display short error messages to the user that looked nicer than TextViews
                 Resource:https://developer.android.com/reference/android/widget/Toast
                 Solution: I used parts of the example code provided.
                 Code Line: 320
                 */

                Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Select two intersections to build a road.", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();

                return;
            }

            //Try to build the road
            tryBuildRoad(selectedIntersections.get(0), selectedIntersections.get(1));
            return;
        }

        // Settlement button on the sidebar.
        if (button.getId() == R.id.sidebar_button_settlement) {
            Log.d(TAG, "onClick: sidebar_button_settlement listener");

            //Make sure an intersection has been selected to build on(Just 1)
            if (selectedIntersections.size() != 1) {
                messageTextView.setText(R.string.one_int_for_set);
                Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Select one intersection to build a settlement.", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();

                shake(messageTextView);
            } else {
                //Build the settlement
                if (tryBuildSettlement(selectedIntersections.get(0))) {
                    this.intersectionOfSettlementSetupTurn = selectedIntersections.get(0);
                    messageTextView.setText(R.string.built_settlement);
                    this.selectedIntersections.clear(); // clear the users selected intersections
                } else {
                    // tell user location is invalid
                    messageTextView.setText(R.string.invalid_set_loc);
                }
            }
            return;
        }

        // City button on the sidebar.
        if (button.getId() == R.id.sidebar_button_city) {

            //Make sure an intersection has been selected to build on(Just 1)
            if (selectedIntersections.size() != 1) {
                messageTextView.setText(R.string.select_one_int_for_city);
                Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Select one intersection to build a city.", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();

            } else {
                Log.e(TAG, "onClick: build city selected intersection: " + selectedIntersections.get(0));

                //Build the city
                if (tryBuildCity(selectedIntersections.get(0))) {
                    messageTextView.setText(R.string.built_city);
                    Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Built a city.", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                } else {
                    messageTextView.setText(R.string.invalid_city_loc);

                }
            }
            return;
        }

        /* ---------------------------- Turn Actions onClick Handlers --------------------------- */

        // Roll button on the sidebar.
        if (button.getId() == R.id.sidebar_button_roll) {
            Log.d(TAG, "onClick: Roll button clicked.");
            // check if it is the players turn
            if (state.getCurrentPlayerId() != this.playerNum) return;
            // check if it is the action phase
            if (state.isActionPhase()) return;
            // send a CatanRollDiceAction to the game
            game.sendAction(new CatanRollDiceAction(this));
            selectedIntersections.clear();
            return;
        }

        // End turn button on the sidebar.
        if (button.getId() == R.id.sidebar_button_endturn) {
            Log.d(TAG, "onClick: End Turn button pressed.");
            state.getCurrentPlayer().getDevCardsBuiltThisTurn().clear();

            //Check if it is setup phase
            if (state.isSetupPhase()) {

                //Make sure player has built enough buildings to end their turn
                // State is authoritative and saved; the temporary list is lost
                // when Android recreates the human player after Continue Game.
                if (state.getCurrentSetupSettlement() < 0
                        || !state.isSetupRoadBuiltThisTurn()) {
                    messageTextView.setText(R.string.build_road_and_set);
                    Toast toast = Toast.makeText(myActivity.getApplicationContext(), R.string.build_road_and_set, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                    shake(messageTextView);
                    return;
                }
            }
            // check if it is the action phase and not the setup phase
            if (!state.isActionPhase() && !state.isSetupPhase()) {
                messageTextView.setText(R.string.cannot_end_turn_before_rolling);
                Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Cannot end turn before rolling!", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
                shake(messageTextView);
                return;
            }
            // check if it is the players turn
            if (state.getCurrentPlayerId() != this.playerNum) return;
            game.sendAction(new CatanEndTurnAction(this));
            hideAllMenusAtEndOfTurn();
            this.buildingsBuiltOnThisTurn = new ArrayList<>(); // reset array list
            selectedIntersections.clear();
            selectedHexagonId = -1;
            return;
        }

        /* -------------------------- Scoreboard and Menu Buttons Handlers ---------------------- */

        // Native scrollable dialogs replace the 2018 fixed-size help pages.
        if (button.getId() == R.id.sidebar_button_menu) {
            showHelpTopicsDialog();
            return;
        }
        // Score button on the sidebar.
        if (button.getId() == R.id.sidebar_button_score)
            toggleGroupVisibilityAllowTapping(scoreBoardGroup);

        /*--------------------------------- Robber onClick --------------------------------*/

        //Robber: Both Move and Steal Phase
        if (button.getId() == R.id.robber_choosehex_confirm) {
            Log.i(TAG, "onClick: Checking if good Hex to place Robber on");
            //Checks if Robber needs to be moved
            if (state.getHasMovedRobber()) {
                //Robber Steal Phase

                //Checks if there is exactly on intersection selected
                if (selectedIntersections.size() != 1) {
                    messageTextView.setText("Please select only one intersection.");
                    return;
                }
                //Checks if intersection actually possesses a building
                if (!state.getBoard().hasBuilding(selectedIntersections.get(0))) {
                    messageTextView.setText(R.string.select_int_w_bldg_robber);
                    return;
                }
                //Checks if building is owned by owner; if so, it rejects the users selection
                if (state.getBoard().getBuildingAtIntersection(selectedIntersections.get(0)).getOwnerId() == playerNum) {
                    messageTextView.setText(R.string.select_int_not_owned_by_you);
                    return;
                }
                //Finally checks if intersection is adjacent to the Hex; if so, send action
                for (Integer intersection : state.getBoard().getHexToIntIdMap().get(state.getBoard().getRobber().getHexagonId())) {
                    if (intersection == selectedIntersections.get(0)) {
                        int stealId = state.getBoard().getBuildingAtIntersection(selectedIntersections.get(0)).getOwnerId();
                        if (state.getPlayerList().get(stealId).getTotalResourceCardCount() < 1) {
                            messageTextView.setText(R.string.no_resources);

                        }
                        game.sendAction(new CatanRobberStealAction(this, playerNum, stealId));
                        robberChooseHexGroup.setVisibility(View.GONE);

                        Toast toast = Toast.makeText(myActivity.getApplicationContext(), "You stole from " + state.getPlayerStealingFrom() + "!", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                        toast.show();
                        return;
                    }
                }
                messageTextView.setText(R.string.select_adjacent_to_robber);
                Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Please select a building adjacent to the Robber tile.", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                //toast.show();
                return;
            }

            //Robber Move Phase: check if we can move the robber to the selected hex
            if (!tryMoveRobber(selectedHexagonId)) {
                Log.e(TAG, "onClick: Error, Not valid Hexagon chosen");
                //robberHexMessage.setText(R.string.invalid_tile);

                return;
            }

            //RobberMoveAction is sent
            Log.i(TAG, "onClick: Successful Hex chosen for Robber, now making group visible");
            robberChooseHexGroup.setVisibility(View.VISIBLE);
            //robberHexMessage.setText("Please selected an intersection with a building adjacent to the robber");
            messageTextView.setText("Please selected an intersection with a building adjacent to the robber");
            boolean hasVictim = false;
            for (Integer adjacent : state.getBoard().getHexToIntIdMap().get(selectedHexagonId)) {
                edu.up.cs.androidcatan.catan.gamestate.buildings.Building building =
                        state.getBoard().getBuildingAtIntersection(adjacent);
                if (building != null && building.getOwnerId() != playerNum) {
                    hasVictim = true;
                    break;
                }
            }
            game.sendAction(new CatanRobberMoveAction(this, playerNum, selectedHexagonId));
            if (!hasVictim) {
                // A victim is optional when no opponent owns an adjacent building.
                game.sendAction(new CatanRobberStealAction(this, playerNum, -1));
                robberChooseHexGroup.setVisibility(View.GONE);
                messageTextView.setText("Robber moved.");
            }
            return;
        }

        //Robber Discard Phase action
        if (button.getId() == R.id.robber_discard_confirm) {

            //Make sure they have the correct number of cards to discard
            if (state.validDiscard(this.playerNum, this.robberDiscardedResources)) {
                messageTextView.setText("Select " + state.getPlayerList().get(this.playerNum).getTotalResourceCardCount() / 2 + " cards to discard.");

                //Activate robber move phase
                if (state.getCurrentPlayerId() == playerNum) {
                    robberChooseHexGroup.setVisibility(View.VISIBLE);
                }

                //Reset values on GUI
                robberBrickAmount.setText(R.string.zero);
                robberLumberAmount.setText(R.string.zero);
                robberGrainAmount.setText(R.string.zero);
                robberOreAmount.setText(R.string.zero);
                robberWoolAmount.setText(R.string.zero);

                //Discard cards
                game.sendAction(new CatanRobberDiscardAction(this, playerNum, robberDiscardedResources));
                this.robberDiscardedResources = state.getRobberDiscardedResources();
                robberDiscardedResources = new int[]{0, 0, 0, 0, 0};

                //Make discard menu disappear
                toggleGroupVisibility(robberDiscardGroup);
                return;
            }

            //Invalid discard, update GUI with needed resources
            int total = 0;
            for (int i = 0; i < this.robberDiscardedResources.length; i++) {
                total += this.robberDiscardedResources[i];
            }
            String message = "You've selected " + total + "/" + state.getPlayerList().get(this.playerNum).getTotalResourceCardCount() / 2 + " resources to discard.";
            messageTextView.setText(message);
            Toast toast = Toast.makeText(myActivity.getApplicationContext(), message, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            shake(messageTextView);
            return;
        }

        //Buttons IDs for Discard Menu
        int robberDiscardAddButtonIds[] = {R.id.robber_discard_brickAddImg, R.id.robber_discard_grainAddImg, R.id.robber_discard_lumberAddImg, R.id.robber_discard_oreAddImg, R.id.robber_discard_woolAddImg};
        int robberDiscardMinusButtonIds[] = {R.id.robber_discard_brickMinusImg, R.id.robber_discard_grainMinusImg, R.id.robber_discard_lumberMinusImg, R.id.robber_discard_oreMinusImg, R.id.robber_discard_woolMinusImg};
        TextView robberAmounts[] = {robberBrickAmount, robberGrainAmount, robberLumberAmount, robberOreAmount, robberWoolAmount};

        //Iterate through add resource buttons to discard
        for (int i = 0; i < robberDiscardAddButtonIds.length; i++) {

            //Found button for specific resource
            if (button.getId() == robberDiscardAddButtonIds[i]) {
                //Player has enough of the specified resource
                if (robberDiscardedResources[i] < state.getPlayerList().get(this.playerNum).getResourceCards()[i]) {
                    robberDiscardedResources[i]++;
                } else { //Player doesn't have enough of specified resource
                    String warning = "You have don't have any more of that resource!";
                    Toast toast = Toast.makeText(myActivity.getApplicationContext(), warning, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                    shake(messageTextView);
                }
                messageTextView.setText("Select " + state.getPlayerList().get(this.playerNum).getTotalResourceCardCount() / 2 + " cards to discard.");
            } else if (button.getId() == robberDiscardMinusButtonIds[i]) { //Check minus button IDs next
                //Remove from count if we're above 0
                if (robberDiscardedResources[i] > 0) {
                    robberDiscardedResources[i]--;
                } else {//Don't let user choose negative resources
                    String warning = "Can't go any lower!";
                    Toast toast = Toast.makeText(myActivity.getApplicationContext(), warning, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                    shake(messageTextView);
                }
                messageTextView.setText("Select " + state.getPlayerList().get(this.playerNum).getTotalResourceCardCount() / 2 + " cards to discard.");
            }
        }

        //Update the resource amounts for discarding
        for (int i = 0; i < robberAmounts.length; i++) {
            robberAmounts[i].setText(String.valueOf(robberDiscardedResources[i]));
        }

        /* ---------------- Pick Resource Card Menu ---------------------- */

        //Monopoly dev card vars
        int monopolyResourceIds[] = {R.id.pickResMenu_brickIcon, R.id.pickResMenu_grainIcon, R.id.pickResMenu_lumberIcon, R.id.pickResMenu_oreIcon, R.id.pickResMenu_woolIcon};
        ImageView monopolySelectionBox[] = {monopolyBrickSelectionBox, monopolyGrainSelectionBox, monopolyLumberSelectionBox, monopolyOreSelectionBox, monopolyWoolSelectionBox};

        //Get selected resource when monopoly/year of plenty is played
        if (selectedDevCard == 2 || selectedDevCard == 3) {
            for (int i = 0; i < monopolyResourceIds.length; i++) {
                if (button.getId() == monopolyResourceIds[i]) selectedResourceId = i;
            }
            for (int i = 0; i < monopolySelectionBox.length; i++) {
                if (i == selectedResourceId) monopolySelectionBox[i].setVisibility(View.VISIBLE);
                else monopolySelectionBox[i].setVisibility(View.INVISIBLE);
            }
        }

        // confirm choose resource button on the pick resource button
        if (button.getId() == R.id.pickResMenu_ConfirmButton) {
            Log.d(TAG, "onClick: Player tried to confirm a monopoly or year of plenty card");
            // make sure they selected a resource
            if (selectedResourceId == -1) {
                messageTextView.setText(R.string.pick_resource);
                shake(messageTextView);
                return;
            }
            // make sure the selected dev card is either a year of plenty or monopoly card
            if (selectedDevCard != 2 && selectedDevCard != 3) {
                Log.e(TAG, "onClick: selected dev card is not 2 or 3");
                toggleGroupVisibilityAllowTapping(pickResourceGroup);
                this.selectedDevCard = -1;
            }
            // send corresponding actions to the game
            if (selectedDevCard == 2) {
                if (firstYearOfPlentyResource < 0) {
                    firstYearOfPlentyResource = selectedResourceId;
                    selectedResourceId = -1;
                    for (ImageView selection : monopolySelectionBox) {
                        selection.setVisibility(View.INVISIBLE);
                    }
                    messageTextView.setText("Select your second Year of Plenty resource.");
                    return;
                }
                game.sendAction(new CatanUseYearOfPlentyCardAction(
                        this, firstYearOfPlentyResource, selectedResourceId));
                firstYearOfPlentyResource = -1;
            }
            if (selectedDevCard == 3)
                game.sendAction(new CatanUseMonopolyCardAction(this, this.selectedResourceId));
            // hide pick resource menu
            toggleGroupVisibilityAllowTapping(pickResourceGroup);
            this.selectedDevCard = -1;
            this.selectedResourceId = -1;
            return;
        }

        /* -------------------- Development Card Button OnClick() Handlers ---------------------- */

        // Development button located on the sidebar. Should only show/hide dev card menu.
        if (button.getId() == R.id.sidebar_button_devcards) {
            toggleGroupVisibilityAllowTapping(developmentGroup); // toggle menu vis.
            return;
        }

        // Use development card button on the dev card menu.
        if (button.getId() == R.id.use_Card) {
            Log.d(TAG, "onClick: Player tapped the use card button.");
            String devCardNames[] = {"Knight Development", "Victory Points Development", "Year of Plenty", "Monopoly", "Road Development"};
            int developmentCardId = -1;
            for (int i = 0; i < devCardNames.length; i++) {
                if (devCardList.getSelectedItem().equals(devCardNames[i])) developmentCardId = i;
            }
            if (developmentCardId < 0) {
                messageTextView.setText("Choose a development card first.");
                return;
            }
            Log.i(TAG, "onClick: Player is using dev card id: " + developmentCardId + " (" + devCardNames[developmentCardId] + ")");

            Log.d(TAG, "onClick: playable dev cards returned: " + state.getCurrentPlayer().getPlayableDevCards());
            if (!state.getCurrentPlayer().getPlayableDevCards().contains(developmentCardId)) {//  .getDevelopmentCards().contains(developmentCardId)) {
                Log.e(TAG, "onClick: player does not have development card. Cannot use.");
                messageTextView.setText(R.string.dont_have_card);
                Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Can not use a Development Card you built this turn!", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
                return;
            } else {
                Log.d(TAG, "onClick: Development Card was removed from hand");

                // knight card
                if (developmentCardId == 0) {
                    game.sendAction(new CatanUseKnightCardAction(this));
                    if (devCards.size() == 1) {
                        toggleGroupVisibilityAllowTapping(developmentGroup);
                    }
                    return;
                }

                // victory point card
                if (developmentCardId == 1) {
                    messageTextView.setText("Victory Point cards score automatically.");
                    return;
                }

                //year of plenty
                if (developmentCardId == 2) {
                    toggleGroupVisibilityAllowTapping(pickResourceGroup);
                    selectedDevCard = 2;
                    firstYearOfPlentyResource = -1;
                    selectedResourceId = -1;
                    messageTextView.setText("Select your first Year of Plenty resource.");
                    return;
                }

                // monopoly
                if (developmentCardId == 3) {
                    toggleGroupVisibility(pickResourceGroup);
                    selectedDevCard = 3;
                }

                // build road card
                if (developmentCardId == 4) {
                    game.sendAction(new CatanUseRoadBuildingCardAction(this));
                    if (devCards.size() == 1) {
                        toggleGroupVisibilityAllowTapping(developmentGroup);
                    }
                    this.rollButton.setAlpha(0.5f);
                    this.rollButton.setClickable(false);
                    this.buildRoadButton.setAlpha(1f);
                    this.buildRoadButton.setClickable(true);
                    this.buildSettlementButton.setAlpha(0.5f);
                    this.buildSettlementButton.setClickable(false);
                    this.buildCityButton.setAlpha(0.5f);
                    this.buildCityButton.setClickable(false);
                    this.sidebarOpenDevCardMenuButton.setAlpha(0.5f);
                    this.sidebarOpenDevCardMenuButton.setClickable(false);
                    this.tradeButton.setAlpha(0.5f);
                    this.tradeButton.setClickable(false);
                    this.endTurnButton.setAlpha(0.5f);
                    this.endTurnButton.setClickable(false);
                    this.sidebarScoreboardButton.setAlpha(0.5f);
                    this.sidebarScoreboardButton.setClickable(false);
                    this.sidebarMenuButton.setAlpha(0.5f);
                    this.sidebarMenuButton.setClickable(false);
                    return;
                }
            }
        }

        // Build development card button in the development card menu.
        if (button.getId() == R.id.build_devCard) {
            // check if player has resources
            if (state.getCurrentPlayer().hasResourceBundle(DevelopmentCard.resourceCost)) {
                // send action to the game
                game.sendAction(new CatanBuyDevCardAction(this));
                //devCardsBuiltThisTurn.add(state.getCurrentPlayer().getDevelopmentCards().get(state.getCurrentPlayer().getDevelopmentCards().size()-1));
                messageTextView.setText(R.string.you_built_a_dev);
            } else {
                messageTextView.setText(R.string.not_enough_for_dev_card);
                Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Not enough resources to build a devlopment.", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
                shake(messageTextView);
            }
            return;
        }

        /* ------------------------------------ Trading ---------------------------------- */

        // A single responsive trade dialog replaces the rigid 26-view overlay.
        if (button.getId() == R.id.sidebar_button_trade) {
            if (selectedIntersections.size() > 1) {
                messageTextView.setText("Select one port intersection, or clear selection for the bank.");
                return;
            }
            showTradeDialog();
            return;
        }

        /* ----------------------- Trade Menu ---------------------------- */

        // arrays of the selection box image views
        ImageView selectionBoxGive[] = {brickSelectionBoxGive, grainSelectionBoxGive, lumberSelectionBoxGive, oreSelectionBoxGive, woolSelectionBoxGive};
        ImageView selectionBoxReceive[] = {brickSelectionBoxReceive, grainSelectionBoxReceive, lumberSelectionBoxReceive, oreSelectionBoxReceive, woolSelectionBoxReceive};

        // set all give selection boxes to transparent
        for (ImageView imageView : selectionBoxGive) {
            imageView.setBackgroundColor(Color.argb(0, 0, 0, 0));
        }
        // set all receive selection boxes to transparent
        for (ImageView imageView : selectionBoxReceive) {
            imageView.setBackgroundColor(Color.argb(0, 0, 0, 0));
        }

        // arrays of the buttons
        int giveButtonIds[] = {R.id.image_trade_menu_give_brick, R.id.image_trade_menu_give_grain, R.id.image_trade_menu_give_lumber, R.id.image_trade_menu_give_ore, R.id.image_trade_menu_give_wool};
        int recButtonIds[] = {R.id.image_trade_menu_rec_brick, R.id.image_trade_menu_rec_grain, R.id.image_trade_menu_rec_lumber, R.id.image_trade_menu_rec_ore, R.id.image_trade_menu_rec_wool};

        //Iterate through trade buttons
        for (int i = 0; i < 5; i++) {
            if (button.getId() == giveButtonIds[i]) {
                tradeGiveSelection = i;
                break;
            }
            if (button.getId() == recButtonIds[i]) {
                tradeReceiveSelection = i;
                break;
            }
        }

        // if the user selects resource to receive -> highlight the selection
        if (tradeReceiveSelection != -1)
            selectionBoxReceive[tradeReceiveSelection].setBackgroundColor(Color.argb(255, 255, 255, 187));

        // if the user selects resource to give -> highlight the selection
        if (tradeGiveSelection != -1)
            selectionBoxGive[tradeGiveSelection].setBackgroundColor(Color.argb(255, 255, 255, 187));

        // confirm trade logic
        if (button.getId() == R.id.button_trade_menu_confirm) {
            Log.i(TAG, "onClick: Player tried to confirm trade");
            Log.i(TAG, "onClick: selected intersections: " + this.selectedIntersections);
            //checks to see if the user has any intersections selected.
            if (selectedIntersections.size() == 1) {
                if (tryTradeWithPort(tradeGiveSelection, tradeReceiveSelection)) {
                    Log.i(TAG, "onClick: traded with port");
                    selectedIntersections.clear();
                    tradeGiveSelection = -1;
                    tradeReceiveSelection = -1;
                } else {
                    Log.w(TAG, "onClick: trade with port failed");
                }
            } else if (selectedIntersections.size() == 0) {
                if (tryTradeWithBank(tradeGiveSelection, tradeReceiveSelection)) {
                    Log.i(TAG, "onClick: traded with bank");
                    selectedIntersections.clear();
                    tradeGiveSelection = -1;
                    tradeReceiveSelection = -1;
                } else {
                    Log.e(TAG, "onClick: trade with bank failed");
                }
            } else {
                selectedIntersections.size();
                Log.e(TAG, "onClick: user has selected too many intersections");
                messageTextView.setText(R.string.less_than_2_res);
            }
        }

        // cancel button on the trade menu
        if (button.getId() == R.id.button_trade_menu_cancel) {
            toggleGroupVisibility(tradeGroup); // hide the trade menu
            messageTextView.setText(R.string.action_phase); // set the info message back to the action phase
            selectedIntersections.clear(); // clear any selected intersections
            tradeReceiveSelection = -1; // reset selections
            tradeGiveSelection = -1;
        }


        /* ----------------------- Help Menus ---------------------------- */
        if (button.getId() == R.id.winning_Help_Button) {
            /**
             External Citation
             Date: 3 December 2018
             Problem: Need music for the app that somewhat explains the game in a fun way.
             Resource: https://www.youtube.com/watch?v=rAJ17ZhmF8M
             Solution: I downloaded the audio from the video.

             Code Line: 850
             */

                menuMusic.setLooping(false);
                menuMusic.setVolume(1f, 1f);
                menuMusic.seekTo(0);
                generalMusic.setVolume(0, 0);
                menuMusic.start();
        }

        //Help Menu Buttons
        int[] buttonIds = new int[]{R.id.winning_Help_Button, R.id.set_Up_Phase_Help_Button, R.id.building_Help_Button, R.id.development_Cards_Help_Button, R.id.trading_Help_Button, R.id.robber_Help_Button};
        Group[] helpMenuGroups = new Group[]{winningHelpMenu, setUpPhaseHelpMenu, buildingHelpMenu, developmentCardHelpMenu, tradingHelpMenu, robberHelpMenu};
        int[] backButtonIds = new int[]{R.id.winning_help_menu_Back, R.id.set_up_phase_help_menu_Back, R.id.building_help_menu_Back, R.id.deleopment_card_help_menu_Back, R.id.trading_help_menu_Back, R.id.robber_help_menu_Back};

        //Iterate through opening menu buttons; open specified menu
        for (int i = 0; i < buttonIds.length; i++) {
            if (button.getId() == buttonIds[i]) {
                sidebarMenuButton.setClickable(false);
                sidebarMenuButton.setAlpha(0.5f);
                toggleGroupVisibilityAllowTapping(helpMenu);
                toggleGroupVisibilityAllowTapping(helpMenuGroups[i]);
                break;
            }
        }

        //Iterate through back buttons for each menu; close menu when pressed
        for (int i = 0; i < backButtonIds.length; i++) {
            if (button.getId() == backButtonIds[i]) {
                sidebarMenuButton.setClickable(true);
                sidebarMenuButton.setAlpha(1.0f);
                toggleGroupVisibilityAllowTapping(helpMenu);
                toggleGroupVisibilityAllowTapping(helpMenuGroups[i]);
                if(i == 0)
                {
                    menuMusic.pause();
                    generalMusic.setVolume(1,1);
                }
                break;
            }
        }

    } // onClick END

    /* ----------------------- BoardSurfaceView Touch Listeners --------------------------------- */

    // the purpose of the touch listener is just to store the touch X,Y coordinates
    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (null == state) return false;
            if (isMenuOpen) return false;
            if (null == state) return false;
            if (playerNum != state.getCurrentPlayerId()) return false;

            // save the X,Y coordinates
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                lastTouchDownXY[0] = boardSurfaceView.toBoardX(event.getX());
                lastTouchDownXY[1] = boardSurfaceView.toBoardY(event.getY());
            }

            // let the touch event pass on to whoever needs it
            return false;
        }
    }; // touchListener END

    // listener that takes the x, y of the touch and converts it into a hex or intersection
    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // retrieve the stored coordinates
            float x = lastTouchDownXY[0];
            float y = lastTouchDownXY[1];
            if (boardSurfaceView == null) return;
            if (y > 100 && y < boardSurfaceView.getHeight() - 100) {
                View decorView = myActivity.getWindow().getDecorView();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
            if (isMenuOpen) return;
            if (state == null) return;
            if (playerNum != state.getCurrentPlayerId()) return;

            boolean touchedIntersection = false;
            boolean touchedHexagon = false;

            HexagonGrid grid = boardSurfaceView.getGrid();
            if (grid == null) return;
            if (grid.getIntersections() == null) return;
            Log.d("TAG", "onLongClick: x = " + x + ", y = " + y); // x, y position

            for (int i = 0; i < grid.getIntersections().length; i++) {
                int xPos = grid.getIntersections()[i].getXPos();
                int yPos = grid.getIntersections()[i].getYPos();

                // if y is greater than y - 25 and less than y + 25
                if (y > yPos - 100 && y < yPos + 100 && x > xPos - 100 && x < xPos + 100) {
                    // if x is greater than point 3 and less than point 0
                    Log.d(TAG, "onClick: Touched intersection id: " + grid.getIntersections()[i].getIntersectionId());
                    touchedIntersection = true;
                    if (grid.getHighlightedIntersections().contains(i)) {
                        boardSurfaceView.getGrid().getHighlightedIntersections().remove((Integer) i);
                        selectedIntersections.remove((Integer) i);
                    } else {
                        boardSurfaceView.getGrid().addHighlightedIntersection(i);
                        if (selectedIntersections.size() > 1) selectedIntersections.remove(0);
                        selectedIntersections.add(i);

                    }
                    boardSurfaceView.getGrid().setHighlightedHexagon(-1);
                    selectedHexagonId = -1;
                    boardSurfaceView.invalidate();
                }
            }
            // if they didn't touch an intersection then check if they touched a hexagon
            if (!touchedIntersection) {
                ArrayList<HexagonDrawable> dHexes = grid.getDrawingHexagons();

                // go through each hexagon and check if the touch matches the bounds of the hex
                int index = 0;
                for (HexagonDrawable hex : dHexes) {
                    int[][] points = hex.getHexagonPoints();

                    // if y is greater than point 0 and less than point 1
                    if (y > points[0][1] && y < points[1][1]) {
                        // if x is greater than point 3 and less than point 0
                        if (x > points[3][0] && x < points[0][0]) {
                            Hexagon dataHexagon = state.getBoard().getHexagonListForDrawing().get(index);
                            Log.w(TAG, "onClick: Touched hexagon id: " + dataHexagon.getHexagonId());
                            touchedHexagon = true;
                            if (dataHexagon.getHexagonId() == boardSurfaceView.getGrid().getHighlightedHexagon()) {
                                // if the hexagon touched is already selected, un-select it
                                boardSurfaceView.getGrid().setHighlightedHexagon(-1);
                                selectedHexagonId = -1;
                            } else {
                                // if touched hexagon is not already selected, select/highlight it
                                boardSurfaceView.getGrid().setHighlightedHexagon(dataHexagon.getHexagonId());
                                selectedHexagonId = dataHexagon.getHexagonId();
                            }
                            boardSurfaceView.getGrid().getHighlightedIntersections().clear();
                            selectedIntersections.clear();
                            boardSurfaceView.invalidate();
                        }
                    }
                    index++;
                }
            }
            // check if no hexagon or intersection was touched (aka. outside the island)
            if (!touchedHexagon && !touchedIntersection) {
                boardSurfaceView.getGrid().setHighlightedHexagon(-1);
                boardSurfaceView.getGrid().getHighlightedIntersections().clear();
                selectedIntersections.clear();
                boardSurfaceView.invalidate();
            }
        }
    }; // clickListener END

    /*--------------------------------------- Action Validation Methods ---------------------------------*/

    /**
     * @param intersectionA First intersection of the road.
     * @param intersectionB Second intersection of the road. (order does not matter)
     * @return If success.
     */
    public boolean tryBuildRoad(int intersectionA, int intersectionB) {
        Log.d(TAG, "tryBuildRoad() called with: intersectionA = [" + intersectionA + "], intersectionB = [" + intersectionB + "]");
        // check if user given intersections are valid
        if (state.getBoard().validRoadPlacement(state.getCurrentPlayerId(), state.isSetupPhase(), intersectionA, intersectionB,
                state.isSetupPhase() ? state.getCurrentSetupSettlement() : -1)) {
            Log.i(TAG, "tryBuildRoad: Valid road placement received.");
        } else {
            messageTextView.setText(R.string.invalid_road_placement);
            Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Invalid road placement.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            Log.d(TAG, "tryBuildRoad() returned: " + false);
            return false;
        }
        // check if it is the setup phase
        if (state.isSetupPhase()) {
            boardSurfaceView.getGrid().clearHighLightedIntersections();
            selectedIntersections.clear(); // clear the selected intersections
            this.buildingsBuiltOnThisTurn.add(0);
            game.sendAction(new CatanBuildRoadAction(this, state.isSetupPhase(), state.getCurrentPlayerId(), intersectionA, intersectionB));
            messageTextView.setText(R.string.road_built);
            return true;
        }
        // if it is not the setup phase, then check if it is the action phase
        if (!state.isActionPhase() && state.getFreeRoadsRemaining() == 0) {
            Log.i(TAG, "tryBuildRoad: Player cannot build road. Not action phase.");
            messageTextView.setText(R.string.roll_the_dice);
            shake(messageTextView);
            return false;
        }
        // if it is not the setup phase check if the player has enough resources to build a road
        if (state.getFreeRoadsRemaining() > 0 ||
                state.getPlayerList().get(state.getCurrentPlayerId()).hasResourceBundle(Road.resourceCost)) {
            // send build settlement action to the game
            Log.d(TAG, "tryBuildRoad: Sending a CatanBuildRoadAction to the game.");
            game.sendAction(new CatanBuildRoadAction(this, state.isSetupPhase(), state.getCurrentPlayerId(), intersectionA, intersectionB));
            // clear selected intersections
            boardSurfaceView.getGrid().clearHighLightedIntersections();
            selectedIntersections.clear(); // clear the selected intersections
            this.buildingsBuiltOnThisTurn.add(0);
            Log.d(TAG, "tryBuildRoad() returned: " + true);
            return true;
        }
        Log.i(TAG, "tryBuildRoad: player does not have enough resources to build a road.");
        messageTextView.setText(R.string.not_enough_for_road);
        Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Not enough resources to build a road.", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
        Log.d(TAG, "tryBuildRoad() returned: " + false);
        return false;
    }

    /**
     * @param intersection1 IntersectionDrawable at which the player is trying to build a settlement upon.
     * @return If the building location chosen is valid, and if the action was carried out.
     */
    private boolean tryBuildSettlement(int intersection1) {
        Log.d(TAG, "tryBuildSettlement() called with: intersection1 = [" + intersection1 + "]");
        // check if valid settlement location
        if (state.getBoard().validBuildingLocation(state.getCurrentPlayerId(), state.isSetupPhase(), intersection1)) {
            Log.d(TAG, "onClick: building location is valid. Sending a BuildSettlementAction to the game.");
            // send build settlement action to the game
            Log.d(TAG, "tryBuildSettlement: Sending a CatanBuildSettlementAction to the game.");
            game.sendAction(new CatanBuildSettlementAction(this, state.isSetupPhase(), state.getCurrentPlayerId(), intersection1));
            this.buildingsBuiltOnThisTurn.add(1);

            Log.d(TAG, "tryBuildSettlement() returned: " + true);
            return true;
        } else {
            messageTextView.setText(R.string.invalid_settlement_loc);
            Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Invalid settlement location.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            shake(messageTextView);
            Log.d(TAG, "tryBuildSettlement: Returning false.");
            return false;
        }
    }

    /**
     * @param intersection Intersection player is attempting to build a city at.
     * @return If a city was built at the intersection.
     */
    private boolean tryBuildCity(int intersection) {
        Log.d(TAG, "tryBuildCity() called with: intersection = [" + intersection + "]");
        // check if it is the setup phase
        if (state.isSetupPhase()) {
            Log.i(TAG, "tryBuildCity: Cannot built city during setup phase. Returning false.");
            return false;
        }
        // check if the player has enough resources
        if (!state.getCurrentPlayer().hasResourceBundle(City.resourceCost)) {
            messageTextView.setText(R.string.not_enough_for_city);
            Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Not enough resources to build a city.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            shake(messageTextView);
            Log.d(TAG, "tryBuildCity() returned: " + false);
            return false;
        }
        // check if valid location
        if (state.getBoard().validCityLocation(state.getCurrentPlayerId(), intersection)) {
            Log.d(TAG, "onClick: building location is valid. Sending a BuildCityAction to the game.");
            // send CatanBuildCityAction to the game
            game.sendAction(new CatanBuildCityAction(this, state.isSetupPhase(), state.getCurrentPlayerId(), intersection));
            this.buildingsBuiltOnThisTurn.add(2);
            this.selectedIntersections.clear();
            return true;
        }
        messageTextView.setText(R.string.invalid_city_loc);
        Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Invaild city location.", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
        shake(messageTextView);
        return false;
    }

    /**
     * @param hexId Hexagon to try to move the robber to.
     * @return Success.
     */
    private boolean tryMoveRobber(int hexId) {
        // Classic rules permit any different hexagon, including the desert.
        if (hexId < 0 || hexId >= state.getBoard().getHexagons().size()
                || hexId == state.getBoard().getRobber().getHexagonId()) {
            messageTextView.setText("Select a different hexagon for the robber.");
            shake(messageTextView);
            return false;
        }
        return true;
    }

    /**
     * @param resourceGiving    Resource the player wants to give in the trade.
     * @param resourceReceiving Resource the player wants to receive in the trade.
     * @return Trade success.
     */
    private boolean tryTradeWithPort(int resourceGiving, int resourceReceiving) {
        //Check if given resource is selected
        if (resourceGiving < 0) {
            messageTextView.setText(R.string.give_res_not_sel);
            Toast toast = Toast.makeText(myActivity.getApplicationContext(), R.string.give_res_not_sel, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            shake(messageTextView);
            return false;
        }

        //Check if received resource is selected
        if (resourceReceiving < 0) {
            messageTextView.setText(R.string.rec_res_not_sel);
            Toast toast = Toast.makeText(myActivity.getApplicationContext(), R.string.rec_res_not_sel, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            shake(messageTextView);
            return false;
        }

        //Check if resources are the same; return false if they are
        if (resourceGiving == resourceReceiving) {
            messageTextView.setText(R.string.unique_res_trading);
            Toast toast = Toast.makeText(myActivity.getApplicationContext(), R.string.unique_res_trading, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            shake(messageTextView);
            return false;
        }

        //Check if port is being traded with
        ArrayList<Port> ports = state.getBoard().getPortList();
        Port tradingWith = null;
        for (Port port : ports) {
            if (port.getIntersectionB() == selectedIntersections.get(0) || port.getIntersectionA() == selectedIntersections.get(0))
                tradingWith = port;
        }
        // make sure selected intersection has port access
        if (tradingWith == null) {
            this.messageTextView.setText(R.string.no_port_access);
            Toast toast = Toast.makeText(myActivity.getApplicationContext(), R.string.no_port_access, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            Log.d(TAG, "tryTradeWithPort() returned: " + false);
            return false;
        }
        // make sure a building is selected
        if (!state.getBoard().hasBuilding(selectedIntersections.get(0))) {
            messageTextView.setText(R.string.port_access);
            shake(messageTextView);
            return false;
        }
        // check if player owns selected building
        if (state.getBoard().getBuildings()[selectedIntersections.get(0)].getOwnerId() != state.getCurrentPlayerId()) {
            messageTextView.setText(R.string.no_port_access);
            shake(messageTextView);
            return false;
        }
        // if trading with a normal port
        if (tradingWith.getResourceId() != -1) {
            // check if player has enough resources
            if (state.getPlayerList().get(state.getCurrentPlayerId()).checkResourceCard(tradingWith.getResourceId(), tradingWith.getTradeRatio())) {
                // send action to the game
                game.sendAction(new CatanTradeWithPortAction(this, tradingWith, resourceReceiving));
                messageTextView.setText(R.string.traded_with_port);
                toggleGroupVisibility(tradeGroup);
                return true;
            } else {
                messageTextView.setText(R.string.not_enough_for_trade);
                Toast toast = Toast.makeText(myActivity.getApplicationContext(), R.string.not_enough_for_trade, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
                shake(messageTextView);
                return false;
            }

        } else { // if the player is trading with a mystery port
            // check if the player has enough resources
            if (state.getPlayerList().get(state.getCurrentPlayerId()).checkResourceCard(resourceGiving, tradingWith.getTradeRatio())) {
                // send action to the game
                game.sendAction(new CatanTradeWithCustomPortAction(this, resourceGiving, resourceReceiving));
                messageTextView.setText(R.string.traded_with_port);
                toggleGroupVisibility(tradeGroup);
                return true;
            } else {
                messageTextView.setText(R.string.not_enough_for_trade);
                Toast toast = Toast.makeText(myActivity.getApplicationContext(), R.string.not_enough_for_trade, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
                shake(messageTextView);
                return false;
            }
        }
    }

    /**
     * @param resourceGiving    Resource to give in the trade with the bank.
     * @param resourceReceiving Resource to receive in the trade with the bank.
     * @return Trade success.
     */
    private boolean tryTradeWithBank(int resourceGiving, int resourceReceiving) {
        Log.d(TAG, "tryTradeWithBank() called with: resourceGiving = [" + resourceGiving + "], resourceReceiving = [" + resourceReceiving + "]");
        if (resourceGiving < 0) { // if resource is not selected
            messageTextView.setText(R.string.give_res_not_sel);
            Toast toast = Toast.makeText(myActivity.getApplicationContext(), R.string.give_res_not_sel, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            shake(messageTextView);
            return false;
        }
        if (resourceReceiving < 0) { // if resource is not selected
            messageTextView.setText(R.string.rec_res_not_sel);
            Toast toast = Toast.makeText(myActivity.getApplicationContext(), R.string.rec_res_not_sel, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            shake(messageTextView);
            return false;
        }
        if (resourceGiving == resourceReceiving) {
            messageTextView.setText(R.string.unique_res_trading);
            Toast toast = Toast.makeText(myActivity.getApplicationContext(), R.string.unique_res_trading, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            shake(messageTextView);
            return false;
        }
        // Check if player has 4 or more of the resource they have selected to give to the bank.
        if (state.getPlayerList().get(state.getCurrentPlayerId()).getResourceCards()[resourceGiving] - 4 >= 0) {
            Log.d(TAG, "tryTradeWithBank: sending CatanTradeWithBankAction to the game.");
            game.sendAction(new CatanTradeWithBankAction(this, resourceGiving, resourceReceiving));
            toggleGroupVisibility(tradeGroup); // show/hide trade menu
            Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Traded with bank.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            Log.i(TAG, "tryTradeWithBank() returned: " + true);
            return true;
        }
        Log.i(TAG, "tryTradeWithBank: player " + state.getPlayerList().get(state.getCurrentPlayerId()) + " does have have enough " + resourceGiving + " to complete trade");
        return false;
    }

    /* ---------------------------------------- GUI Methods --------------------------------------*/

    /**
     * updates all text views a gui components to reflect current game state
     */
    private void updateGameplayHud() {
        if (myActivity == null || state == null || playerNum < 0
                || playerNum >= state.getPlayerList().size()) return;

        int[] hand = state.getPlayerList().get(playerNum).getResourceCards();
        if (hudHandFirst != null) hudHandFirst.setText("Brick " + hand[0]
                + "  ·  Grain " + hand[1] + "  ·  Lumber " + hand[2]);
        if (hudHandSecond != null) hudHandSecond.setText("Ore " + hand[3]
                + "  ·  Wool " + hand[4] + "  ·  Total " +
                state.getPlayerList().get(playerNum).getTotalResourceCardCount());

        if (hudLastRoll != null) {
            int[] dice = state.getDice().getDiceValues();
            if (state.isSetupPhase()) {
                hudLastRoll.setText("Dice: opening placements");
            } else if (dice[0] + dice[1] != state.getCurrentDiceSum()) {
                hudLastRoll.setText("Dice: waiting for first roll");
            } else {
                hudLastRoll.setText("Last roll: " + dice[0] + " + " + dice[1]
                        + " = " + state.getCurrentDiceSum());
            }
        }

        if (hudCards != null) {
            int[] counts = new int[HUD_CARD_NAMES.length];
            for (Integer id : state.getPlayerList().get(playerNum).getDevelopmentCards()) {
                if (id != null && id >= 0 && id < counts.length) counts[id]++;
            }
            StringBuilder cards = new StringBuilder("Your development cards: ");
            int found = 0;
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] == 0) continue;
                if (found++ > 0) cards.append(" · ");
                cards.append(HUD_CARD_NAMES[i]).append(" ×").append(counts[i]);
            }
            if (found == 0) cards.append("None");
            hudCards.setText(cards.toString());
        }
        if (hudLatestAction != null)
            hudLatestAction.setText("Latest: " + CatanMatchHistory.latest(myActivity));
    }

    private void showGameplayHistory() {
        if (myActivity == null) return;
        String all = CatanMatchHistory.history(myActivity);
        // Show recent events first in a readable, scrollable native dialog.
        String[] lines = all.isEmpty() ? new String[0] : all.split("\\n");
        StringBuilder readable = new StringBuilder();
        for (int i = lines.length - 1; i >= 0; i--) {
            if (!lines[i].trim().isEmpty())
                readable.append("• ").append(lines[i]).append("\\n\\n");
        }
        new AlertDialog.Builder(myActivity)
                .setTitle("Match history — latest first")
                .setMessage(readable.length() == 0
                        ? "Rolls, building, trading and development cards will appear here."
                        : readable.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void updateTextViews() {

        View decorView = myActivity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);

        // Check if the Game State is null. If it is return void.
        if (this.state == null) {
            Log.e(TAG, "updateTextViews: state is null. Returning void.");
            return;
        }

        // array of dev card names
        String devCardNames[] = {"Knight Development", "Victory Points Development", "Year of Plenty", "Monopoly", "Road Development"};
        // if dev card list is not empty, clear it
        if (!devCards.isEmpty()) devCards.clear();

        // for each dev card the player owns, add it to the gui dev card list
        for (int i = 0; i < state.getPlayerList().get(this.playerNum).getDevelopmentCards().size(); i++) {
            devCards.add(devCardNames[state.getPlayerList().get(this.playerNum).getDevelopmentCards().get(i)]);
        }

        // dev card list for the spinner
        List<String> spinnerList = new ArrayList<>(devCards);

        // if the spinner list is empty then grey out the use dev card button and make it not clickable
        if (spinnerList.size() == 0) {
            this.useDevCard.setAlpha(0.5f);
            this.useDevCard.setClickable(false);
        } else {
            this.useDevCard.setAlpha(1f);
            this.useDevCard.setClickable(true);
        }

        // dev card spinner adapter
        ArrayAdapter<String> devCardSpinnerAdapter = new ArrayAdapter<>(myActivity, R.layout.support_simple_spinner_dropdown_item, spinnerList);
        devCardSpinnerAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        devCardList.setAdapter(devCardSpinnerAdapter); // Apply the adapter to the spinner

        // array of dice image ids
        int diceImageIds[] = {R.drawable.dice_1, R.drawable.dice_2, R.drawable.dice_3, R.drawable.dice_4, R.drawable.dice_5, R.drawable.dice_6};

        // set the dice ImageViews to the corresponding dice image of the current dice values
        diceImageLeft.setBackgroundResource(diceImageIds[state.getDice().getDiceValues()[0] - 1]);
        diceImageRight.setBackgroundResource(diceImageIds[state.getDice().getDiceValues()[1] - 1]);

        // if it is the robber phase
        if (this.state.getRobberPhase()) {

            this.messageTextView.setText(R.string.robber_phase);
            // if it is the setup phase, grey out some buttons and make them un clickable
            this.buildRoadButton.setAlpha(0.5f);
            this.buildRoadButton.setClickable(false);
            this.buildSettlementButton.setAlpha(0.5f);
            this.buildSettlementButton.setClickable(false);
            this.buildCityButton.setAlpha(0.5f);
            this.buildCityButton.setClickable(false);
            this.rollButton.setAlpha(0.5f);
            this.rollButton.setClickable(false);
            this.sidebarOpenDevCardMenuButton.setAlpha(0.5f);
            this.sidebarOpenDevCardMenuButton.setClickable(false);
            this.tradeButton.setAlpha(0.5f);
            this.tradeButton.setClickable(false);
            this.endTurnButton.setAlpha(0.5f);
            this.endTurnButton.setClickable(false);

            //Player Discard Phase
            if (!state.getRobberPlayerListHasDiscarded()[playerNum]) {
                Log.d(TAG, "updateTextViews: Has not discarded cards");
                robberDiscardMessage.setText(String.format(myActivity.getString(R.string.select_n_cards_to_discard), state.getPlayerList().get(this.playerNum).getTotalResourceCardCount() / 2));
                robberDiscardGroup.setVisibility(View.VISIBLE);
                isMenuOpen = true;
                messageTextView.setText(R.string.select_half);
            }

            //TextView for when HumanPlayer's turn
            //Move Robber
            else if (state.getCurrentPlayerId() == playerNum && state.allPlayersHaveDiscarded() && !state.getHasMovedRobber()) {
                Log.d(TAG, "updateTextViews: Now needs to move Robber");
                messageTextView.setText(R.string.move_robber_to_hex);
                robberChooseHexGroup.setVisibility(View.VISIBLE);
                //Robber Steal Phase
            } else if (state.getCurrentPlayerId() == playerNum && state.getHasMovedRobber()) { // todo
                Log.d(TAG, "updateTextViews: Now needs to steal Robber");
                messageTextView.setText(R.string.robber_select_intersection_tosteal);
                robberChooseHexGroup.setVisibility(View.VISIBLE);
            }

            //TextViews when NOT HumanPlayer's Turn
            else if (state.getCurrentPlayerId() != playerNum && !state.allPlayersHaveDiscarded()) {
                Log.d(TAG, "updateTextViews: Waiting for players to discard cards");
                messageTextView.setText(R.string.waiting_for_discard);
            }
            //Other Player Moving Robber
            else if (state.getCurrentPlayerId() != playerNum && state.allPlayersHaveDiscarded() && !state.getHasMovedRobber()) {
                Log.d(TAG, "updateTextViews: Other player needs to move Robber");
                messageTextView.setText(R.string.cpu_move_robber_to_hex);
            }
            //Other Player Stealing
            else if (state.getCurrentPlayerId() != playerNum && state.getHasMovedRobber()) {
                Log.d(TAG, "updateTextViews: Now needs to steal Robber");
                messageTextView.setText(R.string.cpu_robber_steal_phase);
            }

        } else if (this.state.isSetupPhase()) { // IF SETUP PHASE
            this.messageTextView.setText(R.string.setup_phase); // set info message
            // get settlement and road count for the current turn
            // Setup progress must survive an app restart or Continue Game.
            // The temporary UI building list is not a reliable source.
            int settlements = state.getCurrentSetupSettlement() >= 0 ? 1 : 0;
            int roads = state.isSetupRoadBuiltThisTurn() ? 1 : 0;

            // check if they are done with their setup phase turn
            if (settlements == 1 && roads == 1) {
                // they need to end their turn
                this.endTurnButton.setAlpha(1f);
                this.endTurnButton.setClickable(true);
                this.messageTextView.setText(R.string.setup_phase_complete);
            } else {
                this.endTurnButton.setAlpha(0.5f);
                this.endTurnButton.setClickable(false);
            }
            // check if they have built a settlement but not a road
            if (settlements > roads) {
                // they need to build a road
                this.buildRoadButton.setAlpha(1f);
                this.buildRoadButton.setClickable(true);
                this.buildSettlementButton.setAlpha(0.5f);
                this.buildSettlementButton.setClickable(false);
            } else if (settlements == 1) {
                // they need to end their turn
                this.buildRoadButton.setAlpha(0.5f);
                this.buildRoadButton.setClickable(false);
                this.buildSettlementButton.setAlpha(0.5f);
                this.buildSettlementButton.setClickable(false);
            } else {
                // they need to build a settlement
                this.buildRoadButton.setAlpha(0.5f);
                this.buildRoadButton.setClickable(false);
                this.buildSettlementButton.setAlpha(1f);
                this.buildSettlementButton.setClickable(true);
            }
            this.buildCityButton.setAlpha(0.5f);
            this.buildCityButton.setClickable(false);
            this.rollButton.setAlpha(0.5f);
            this.rollButton.setClickable(false);
            this.sidebarOpenDevCardMenuButton.setAlpha(0.5f);
            this.sidebarOpenDevCardMenuButton.setClickable(false);
            this.tradeButton.setAlpha(0.5f);
            this.tradeButton.setClickable(false);

        } else if (!state.isActionPhase()) { // IF NOT THE ACTION PHASE AND NOT THE SETUP PHASE

            if (this.playerNum == state.getCurrentPlayerId())
                this.messageTextView.setText(R.string.roll_the_dice);
            else
                messageTextView.setText(String.format("It is %s's turn.", allPlayerNames[state.getCurrentPlayerId()]));

            // set the roll button only as available
            this.rollButton.setAlpha(1f);
            this.rollButton.setClickable(true);

            // end turn button
            this.endTurnButton.setAlpha(0.5f);
            this.endTurnButton.setClickable(false);

            // build road button
            this.buildRoadButton.setAlpha(0.5f);
            this.buildRoadButton.setClickable(false);

            // build settlement button
            this.buildSettlementButton.setAlpha(0.5f);
            this.buildSettlementButton.setClickable(false);

            // build city button
            this.buildCityButton.setAlpha(0.5f);
            this.buildCityButton.setClickable(false);

            // Action cards can be played before rolling the dice.
            boolean canPlayCard = !state.hasPlayedDevelopmentCardThisTurn()
                    && !state.getPlayerList().get(this.playerNum).getPlayableDevCards().isEmpty();
            this.sidebarOpenDevCardMenuButton.setAlpha(canPlayCard ? 1f : 0.5f);
            this.sidebarOpenDevCardMenuButton.setClickable(canPlayCard);

            // Road Building's two free roads can be laid before rolling.
            if (state.getFreeRoadsRemaining() > 0) {
                this.buildRoadButton.setAlpha(1f);
                this.buildRoadButton.setClickable(true);
            }

            // trade button on sidebar
            this.tradeButton.setAlpha(0.5f);
            this.tradeButton.setClickable(false);

        } else { // ACTION PHASE AND NOT SETUP PHASE
            if (this.playerNum == state.getCurrentPlayerId())
                this.messageTextView.setText(R.string.action_phase);
            setAllButtonsToVisible();
            this.rollButton.setAlpha(0.5f);
            this.rollButton.setClickable(false);
        }
        //Not
        if (this.playerNum != state.getCurrentPlayerId()) {
            this.rollButton.setAlpha(0.5f);
            this.rollButton.setClickable(false);
            this.buildRoadButton.setAlpha(0.5f);
            this.buildRoadButton.setClickable(false);
            this.buildSettlementButton.setAlpha(0.5f);
            this.buildSettlementButton.setClickable(false);
            this.buildCityButton.setAlpha(0.5f);
            this.buildCityButton.setClickable(false);
            this.sidebarOpenDevCardMenuButton.setAlpha(0.5f);
            this.sidebarOpenDevCardMenuButton.setClickable(false);
            this.tradeButton.setAlpha(0.5f);
            this.tradeButton.setClickable(false);
            this.endTurnButton.setAlpha(0.5f);
            this.endTurnButton.setClickable(false);
            this.sidebarScoreboardButton.setAlpha(1f);
            this.sidebarScoreboardButton.setClickable(true);
            this.sidebarMenuButton.setAlpha(1f);
            this.sidebarMenuButton.setClickable(true);
        }

        // Clear turn indicator remains visible even when controls collapse.
        TextView turnChip = myActivity.findViewById(R.id.turn_status_chip);
        if (turnChip != null) {
            String who = getAllPlayerNames()[state.getCurrentPlayerId()];
            String stage = state.isSetupPhase() ? "Placement"
                    : state.isRobberPhase() ? "Robber"
                    : state.isActionPhase() ? "Build / trade" : "Roll";
            turnChip.setText(who + " · " + stage);
            turnChip.setTextColor(HexagonGrid.playerColors[state.getCurrentPlayerId()]);
        }

        /* ----- update resource value TextViews ----- */

        // get the players resources
        int[] resourceCards = this.state.getPlayerList().get(this.playerNum).getResourceCards();
        // go through resources and set corresponding values on the gui
        for (int i = 0; i < resourceCards.length; i++) {
            this.resourceValues[i].setText(String.valueOf(resourceCards[i]));
        }
        if (handStatusChip != null) {
            handStatusChip.setText("YOUR HAND  ·  Brick " + resourceCards[0]
                    + "  ·  Grain " + resourceCards[1]
                    + "  ·  Lumber " + resourceCards[2]
                    + "  ·  Ore " + resourceCards[3]
                    + "  ·  Wool " + resourceCards[4]
                    + "  ·  Dev "
                    + state.getPlayerList().get(this.playerNum).getDevelopmentCards().size());
            handStatusChip.setOnClickListener(v -> showOwnHand());
        }

        /* ----- update scoreboard ----- */

        // set the other players score on the scoreboard to their public scores except for the user which shows their private score



        for (int i = 0; i < 4; i++) {
            //First check if player has any trophies
            int addToThisPlayer = 0;
            if (i == state.getCurrentLargestArmyPlayerId()) addToThisPlayer += 2;
            if (i == state.getCurrentLongestRoadPlayerId()) addToThisPlayer += 2;

            this.playerScores[i].setTextColor(HexagonGrid.playerColors[i]);
            if (i != this.playerNum)
                this.playerScores[i].setText(String.valueOf(state.getPlayerList().get(i).getVictoryPoints()+ addToThisPlayer));
            else
                this.playerScores[this.playerNum].setText(String.valueOf(state.getPlayerList().get(this.playerNum).getVictoryPointsPrivate() + addToThisPlayer + state.getPlayerList().get(this.playerNum).getVictoryPoints()));
        }

        // go through each player name
        for (int i = 0; i < this.playerNameTextViews.length; i++) {
            this.playerNameTextViews[i].setText(getAllPlayerNames()[i]); // set text to name
            this.playerNameTextViews[i].setTextColor(HexagonGrid.playerColors[i]); // set color of player
            if (i == state.getCurrentPlayerId()) {
                this.playerNameTextViews[i].setBackgroundColor(Color.argb(120, 255, 255, 255));
                this.playerScores[i].setBackgroundColor(Color.argb(120, 255, 255, 255));
            } else {
                this.playerNameTextViews[i].setBackgroundColor(Color.TRANSPARENT);
                this.playerScores[i].setBackgroundColor(Color.TRANSPARENT);
            }
        }

        /* ----- update misc. sidebar TextViews ----- */

        this.playerNameSidebar.setText(getAllPlayerNames()[this.playerNum]);

        // human player score (sidebar menu)

        //First check if player has any trophies
        int addToThisPlayer = 0;
        if (this.playerNum == state.getCurrentLargestArmyPlayerId()) addToThisPlayer += 2;
        if (this.playerNum == state.getCurrentLongestRoadPlayerId()) addToThisPlayer += 2;

        //Personal Score with Private VPs
        this.myScore.setText(String.format("VPs: %s", String.valueOf(state.getPlayerList().get(this.playerNum).getVictoryPointsPrivate() + addToThisPlayer + state.getPlayerList().get(this.playerNum).getVictoryPoints())));

        // current turn indicator (sidebar menu)
        this.currentTurnIdTextView.setText(String.valueOf(getAllPlayerNames()[state.getCurrentPlayerId()]));
        this.currentTurnIdTextView.setTextColor(HexagonGrid.playerColors[state.getCurrentPlayerId()]);
        this.playerNameSidebar.setTextColor(HexagonGrid.playerColors[this.playerNum]);

        /* -------- animations ----------- */

        if (this.state.getCurrentPlayerId() == this.playerNum && !this.state.isActionPhase())
            this.playerNameSidebar = (TextView) blinkAnimation(this.playerNameSidebar);

        /* ------------- trophies ------------- */

        armyTrophyImages = new ImageView[]{myActivity.findViewById(R.id.largest_army_player0), myActivity.findViewById(R.id.largest_army_player1), myActivity.findViewById(R.id.largest_army_player2), myActivity.findViewById(R.id.largest_army_player3)};
        roadTrophyImages = new ImageView[]{myActivity.findViewById(R.id.longest_road_player0), myActivity.findViewById(R.id.longest_road_player1), myActivity.findViewById(R.id.longest_road_player2), myActivity.findViewById(R.id.longest_road_player3)};
        showLongestRoadTrophy(state.getCurrentLongestRoadPlayerId());
        showLargestArmyTrophy(state.getCurrentLargestArmyPlayerId());
    } // updateTextViews END

    /** Keep the latest sixty public actions; no opponent secrets are logged. */
    private void addActivityEntry(String entry) {
        if (entry == null || entry.trim().isEmpty()) return;
        activityLog.add(0, entry);
        if (activityLog.size() > 60) activityLog.remove(activityLog.size() - 1);
        if (lastActionChip != null) lastActionChip.setText(entry);
    }

    private void captureOwnResourceChanges() {
        int[] next = state.getPlayerList().get(playerNum).getResourceCards();
        if (lastOwnResources != null) {
            StringBuilder change = new StringBuilder();
            for (int resource = 0; resource < 5; resource++) {
                int delta = next[resource] - lastOwnResources[resource];
                if (delta == 0) continue;
                if (change.length() > 0) change.append(", ");
                change.append(delta > 0 ? "+" : "").append(delta)
                        .append(" ").append(HAND_RESOURCES[resource]);
            }
            if (change.length() > 0)
                addActivityEntry("Your resources: " + change);
        }
        lastOwnResources = next.clone();
    }

    private void showActivityHistory() {
        StringBuilder entries = new StringBuilder();
        for (String entry : activityLog) {
            entries.append("• ").append(entry).append("\\n\\n");
        }
        if (entries.length() == 0) entries.append("No moves recorded yet.");
        TextView content = new TextView(myActivity);
        int pad = (int) (20 * myActivity.getResources().getDisplayMetrics().density);
        content.setPadding(pad, pad, pad, pad);
        content.setTextSize(16);
        content.setText(entries.toString());
        ScrollView scroll = new ScrollView(myActivity);
        scroll.addView(content);
        new AlertDialog.Builder(myActivity)
                .setTitle("Match activity — newest first")
                .setView(scroll)
                .setPositiveButton("Close", null).show();
    }

    private void showOwnHand() {
        if (state == null) return;
        Player self = state.getPlayerList().get(playerNum);
        int[] cards = self.getResourceCards();
        StringBuilder detail = new StringBuilder("RESOURCE CARDS\\n");
        int total = 0;
        for (int i = 0; i < 5; i++) {
            detail.append(HAND_RESOURCES[i]).append(": ").append(cards[i]).append("\\n");
            total += cards[i];
        }
        detail.append("Total: ").append(total).append("\\n\\nDEVELOPMENT CARDS\\n");
        if (self.getDevelopmentCards().isEmpty()) {
            detail.append("None");
        } else {
            String[] types = {"Knight", "Victory Point", "Year of Plenty", "Monopoly", "Road Building"};
            for (int id : self.getDevelopmentCards()) {
                if (id >= 0 && id < types.length)
                    detail.append("• ").append(types[id])
                            .append(id == 1 ? " (scores automatically)" : "")
                            .append("\\n");
            }
        }
        new AlertDialog.Builder(myActivity).setTitle("Your hand")
                .setMessage(detail.toString())
                .setPositiveButton("Close", null).show();
    }

    /**
     * callback method when we get a message (e.g., from the game)
     *
     * @param info the message
     */
    @Override
    public void receiveInfo(GameInfo info) {
        Log.d(TAG, "receiveInfo() called");
        if (debugMode)
            Log.i(TAG, "receiveInfo() called with: info: \n" + info.toString() + "----------------------------");

        if (info instanceof CatanPublicEventInfo) {
            CatanPublicEventInfo event = (CatanPublicEventInfo) info;
            addActivityEntry(event.getDescription());
            if (event.isDiceRoll() && diceStatusChip != null)
                diceStatusChip.setText(event.getDescription());
            return; // Public notices must NOT erase selected roads or intersections.
        }
        if (info instanceof CatanPublicEventInfo) {
            if (myActivity != null) {
                CatanMatchHistory.append(myActivity,
                        ((CatanPublicEventInfo) info).getDescription());
                updateGameplayHud();
            }
            return;
        }
        if (this.boardSurfaceView == null) {
            Log.e(TAG, "receiveInfo: boardSurfaceView is null.");
            return;
        }
        selectedIntersections.clear();

        if (info instanceof CatanGameState) {

            this.state = new CatanGameState((CatanGameState) info);
            captureOwnResourceChanges();
            if (activityLog.isEmpty()) {
                addActivityEntry("Match ready — " + allPlayerNames[state.getCurrentPlayerId()]
                        + " to play");
                if (diceStatusChip != null && !state.isSetupPhase() && state.isActionPhase()) {
                    int[] dice = state.getDice().getDiceValues();
                    diceStatusChip.setText("Current turn: " + dice[0] + " + "
                            + dice[1] + " = " + state.getCurrentDiceSum());
                }
            }
            // set resource count TextViews to the players resource inventory amounts
            Log.i(TAG, "receiveInfo: player list: " + ((CatanGameState) info).getPlayerList());
            Log.i(TAG, "receiveInfo: info.toString " + state.toString());

            if (state.isRobberPhase()) {
                Log.i(TAG, "receiveInfo: it is the robber phase");
                messageTextView.setText(R.string.robber_phase);
                if (state.checkIfPlayerHasDiscarded(playerNum)) {
                    //The player either needs to discard OR they've already finished their discard phase
                    Log.i(TAG, "receiveInfo: Player " + playerNum + " discard status: " + state.getRobberPlayerListHasDiscarded()[playerNum]);
                } else {
                    //Player has not yet gone through discard phase BUT does not need to discard
                    Log.i(TAG, "receiveInfo: Player " + playerNum + " does not need to discard; now sending action");
                    Log.i(TAG, "receiveInfo: sending a CatanRobberDiscardAction to the game playerNum=" + this.playerNum);
                    game.sendAction(new CatanRobberDiscardAction(this, playerNum, new int[]{0, 0, 0, 0, 0}));
                }
            }

            updateTextViews();
            updateGameplayHud();
            fitPopupOnScreen(robberDiscardGroup);
            fitPopupOnScreen(robberChooseHexGroup);
            fitPopupOnScreen(developmentGroup);
            fitPopupOnScreen(pickResourceGroup);
            drawGraphics();

        } else if (info instanceof NotYourTurnInfo) {
            Log.i(TAG, "receiveInfo: Player tried to make action but it is not their turn.");
        } else if (info instanceof IllegalMoveInfo) {
            Log.i(TAG, "receiveInfo: Illegal move info received.");
        } else {
            Log.e(TAG, "receiveInfo: Received instanceof not anything we know. Returning void.");
        }
    }//receiveInfo

    /**
     * callback method--our game has been chosen/re-chosen to be the GUI,
     * called from the GUI thread
     *
     * @param activity the activity under which we are running
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setAsGui(GameMainActivity activity) {
        Log.d(TAG, "setAsGui() called with: activity = [" + activity + "]");




        myActivity = activity; // remember the activity
        initialStateDisplayed = false;

        activity.setContentView(R.layout.catan_main_activity); // Load the layout resource for our GUI

        /**
         External Citation
         Data: 3 December 2018
         Problem: Needed background music for the app from the Catan game
         Resource: https://www.youtube.com/watch?v=Ms3xkkcReuE
         Solution: I downloaded the audio from the video.

         Code Line: 1714
         */
if(newGame) {
        menuMusic = MediaPlayer.create(myActivity .getApplicationContext(),R.raw.settlers_of_catan_official_theme_song);
        generalMusic = MediaPlayer.create(myActivity.getApplicationContext(), R.raw.the_score_of_catan_full_song);
        generalMusic.setLooping(true);
        generalMusic.setVolume(1, 1);
        generalMusic.start();newGame = false;
        }

        if (readyToDraw) {
            myActivity.setContentView(R.layout.catan_main_activity); // Load the layout resource for our GUI
            messageTextView = activity.findViewById(R.id.textview_game_message);
        } else {
            Log.i(TAG, "setAsGui: Loading screen...");
            myActivity.setContentView(R.layout.catan_loading_screen);
            return;
        }

        /* ---------- Surface View for drawing the graphics ----------- */

        diceStatusChip = activity.findViewById(R.id.dice_result_chip);
        lastActionChip = activity.findViewById(R.id.last_action_chip);
        handStatusChip = activity.findViewById(R.id.hand_status_chip);
        Button history = activity.findViewById(R.id.activity_history_button);
        if (history != null) history.setOnClickListener(v -> showActivityHistory());
        if (!activityLog.isEmpty() && lastActionChip != null)
            lastActionChip.setText(activityLog.get(0));
        this.boardSurfaceView = activity.findViewById(R.id.board); // boardSurfaceView board is the custom SurfaceView
        this.boardSurfaceView.setOnClickListener(clickListener);
        this.boardSurfaceView.setOnTouchListener(touchListener);

        /* ----------------------------------- SIDEBAR ------------------------------------------ *

        /* ------------------------ Building Buttons -----------------------------------------*/
        buildRoadButton = activity.findViewById(R.id.sidebar_button_road);
        buildRoadButton.setOnClickListener(this);

        buildSettlementButton = activity.findViewById(R.id.sidebar_button_settlement);
        buildSettlementButton.setOnClickListener(this);

        buildCityButton = activity.findViewById(R.id.sidebar_button_city);
        buildCityButton.setOnClickListener(this);


        // action buttons
        sidebarOpenDevCardMenuButton = activity.findViewById(R.id.sidebar_button_devcards); // buy dev card
        sidebarOpenDevCardMenuButton.setOnClickListener(this);

        tradeButton = activity.findViewById(R.id.sidebar_button_trade); // trade
        tradeButton.setOnClickListener(this);

        /*--------------------Robber Buttons and Groups------------------------*/

        robberBrickPlus = activity.findViewById(R.id.robber_discard_brickAddImg);
        robberBrickMinus = activity.findViewById(R.id.robber_discard_brickMinusImg);
        robberLumberPlus = activity.findViewById(R.id.robber_discard_lumberAddImg);
        robberLumberMinus = activity.findViewById(R.id.robber_discard_lumberMinusImg);
        robberGrainPlus = activity.findViewById(R.id.robber_discard_grainAddImg);
        robberGrainMinus = activity.findViewById(R.id.robber_discard_grainMinusImg);
        robberOrePlus = activity.findViewById(R.id.robber_discard_oreAddImg);
        robberOreMinus = activity.findViewById(R.id.robber_discard_oreMinusImg);
        robberWoolPlus = activity.findViewById(R.id.robber_discard_woolAddImg);
        robberWoolMinus = activity.findViewById(R.id.robber_discard_woolMinusImg);
        robberDiscardMessage = activity.findViewById(R.id.robber_discard_selectMoreResources);
        robberDiscardGroup = activity.findViewById(R.id.robber_discard_group);
        robberConfirmDiscard = activity.findViewById(R.id.robber_discard_confirm);

        robberBrickAmount = activity.findViewById(R.id.robber_discard_brickAmount);
        robberLumberAmount = activity.findViewById(R.id.robber_discard_lumberAmount);
        robberGrainAmount = activity.findViewById(R.id.robber_discard_grainAmount);
        robberOreAmount = activity.findViewById(R.id.robber_discard_oreAmount);
        robberWoolAmount = activity.findViewById(R.id.robber_discard_woolAmount);

        Button robberConfirmHex = activity.findViewById(R.id.robber_choosehex_confirm);
        robberHexMessage = activity.findViewById(R.id.robber_choosehex_message);
        //robberHexMessage.setText(R.string.choose_robber_tile);
        robberChooseHexGroup = activity.findViewById(R.id.robber_choosehex_menu);

        robberBrickPlus.setOnClickListener(this);
        robberBrickMinus.setOnClickListener(this);
        robberLumberPlus.setOnClickListener(this);
        robberLumberMinus.setOnClickListener(this);
        robberGrainPlus.setOnClickListener(this);
        robberGrainMinus.setOnClickListener(this);
        robberOrePlus.setOnClickListener(this);
        robberOreMinus.setOnClickListener(this);
        robberWoolPlus.setOnClickListener(this);
        robberWoolMinus.setOnClickListener(this);

        robberConfirmDiscard.setOnClickListener(this);
        robberConfirmHex.setOnClickListener(this);

        rollButton = activity.findViewById(R.id.sidebar_button_roll); // Roll button
        rollButton.setOnClickListener(this);
        endTurnButton = activity.findViewById(R.id.sidebar_button_endturn); // End Turn button
        endTurnButton.setOnClickListener(this);
        diceImageLeft = activity.findViewById(R.id.diceImageLeft);  //dice roll images
        diceImageRight = activity.findViewById(R.id.diceImageRight);

        /* ---------- Sidebar resource values ---------- */
        this.resourceValues = new TextView[]{activity.findViewById(R.id.sidebar_value_brick), activity.findViewById(R.id.sidebar_value_grain), activity.findViewById(R.id.sidebar_value_lumber), activity.findViewById(R.id.sidebar_value_ore), activity.findViewById(R.id.sidebar_value_wool)};

        /* ---------- misc sidebar buttons and text views ---------- */

        this.sidebarMenuButton = activity.findViewById(R.id.sidebar_button_menu);
        this.sidebarMenuButton.setOnClickListener(this);
        this.buildingCosts = activity.findViewById(R.id.building_costs);

        this.sidebarScoreboardButton = activity.findViewById(R.id.sidebar_button_score);
        this.sidebarScoreboardButton.setOnClickListener(this);

        this.myScore = activity.findViewById(R.id.sidebar_heading_vp);
        this.currentTurnIdTextView = activity.findViewById(R.id.sidebar_heading_current_turn);
        this.playerNameSidebar = activity.findViewById(R.id.sidebar_heading_playername);

        /* ------------------- Help Menu Buttons and Groups -------------------- */
        this.winningHelpButton = activity.findViewById(R.id.winning_Help_Button);
        this.winningHelpButton.setOnClickListener(this);

        this.setUpPhaseHelpButton = activity.findViewById(R.id.set_Up_Phase_Help_Button);
        this.setUpPhaseHelpButton.setOnClickListener(this);

        this.buildingHelpButtonm = activity.findViewById(R.id.building_Help_Button);
        this.buildingHelpButtonm.setOnClickListener(this);

        this.developmentCardHelpButon = activity.findViewById(R.id.development_Cards_Help_Button);
        this.developmentCardHelpButon.setOnClickListener(this);

        this.tradingHelpButton = activity.findViewById(R.id.trading_Help_Button);
        this.tradingHelpButton.setOnClickListener(this);

        this.robberHelpButton = activity.findViewById(R.id.robber_Help_Button);
        this.robberHelpButton.setOnClickListener(this);

        this.winningHelpBackButton = activity.findViewById(R.id.winning_help_menu_Back);
        this.winningHelpBackButton.setOnClickListener(this);

        this.setUpPhaseHelpBackButton = activity.findViewById(R.id.set_up_phase_help_menu_Back);
        this.setUpPhaseHelpBackButton.setOnClickListener(this);

        this.buildingHelpBackButton = activity.findViewById(R.id.building_help_menu_Back);
        this.buildingHelpBackButton.setOnClickListener(this);

        this.developmentCardHelpBackButton = activity.findViewById(R.id.deleopment_card_help_menu_Back);
        this.developmentCardHelpBackButton.setOnClickListener(this);

        this.tradingHelpBackButton = activity.findViewById(R.id.trading_help_menu_Back);
        this.tradingHelpBackButton.setOnClickListener(this);

        this.robberHelpBackButton = activity.findViewById(R.id.robber_help_menu_Back);
        this.robberHelpBackButton.setOnClickListener(this);

        this.helpMenu = activity.findViewById(R.id.help_menu_group);
        this.buildingHelpMenu = activity.findViewById(R.id.building_help_menu_group);
        this.developmentCardHelpMenu = activity.findViewById(R.id.development_card_help_menu_group);
        this.winningHelpMenu = activity.findViewById(R.id.winning_help_menu_group);
        this.setUpPhaseHelpMenu = activity.findViewById(R.id.set_up_phase_help_menu_group);
        this.tradingHelpMenu = activity.findViewById(R.id.trading_help_menu_group);
        this.robberHelpMenu = activity.findViewById(R.id.robber_help_menu_group);

        /* ------------ DEV CARD SPINNER ----------------- */

        devcard_text_info = activity.findViewById(R.id.development_Card_Info);
        devCardList = activity.findViewById(R.id.development_Card_Spinner); // DEV CARD SPINNER

        devCardList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String devCardNames[] = {"Knight Development", "Victory Points Development", "Year of Plenty", "Monopoly", "Road Development"};

                int devCardId = -1;
                for (int i = 0; i < devCardNames.length; i++) {
                    if (devCardNames[i].equals(devCards.get(position))) devCardId = i;
                }
                switch (devCardId) {
                    case 0:
                        devcard_text_info.setText(R.string.knight_info);
                        break;
                    case 1:
                        devcard_text_info.setText(R.string.victory_point_info);
                        break;
                    case 2:
                        devcard_text_info.setText(R.string.year_of_plenty_name);
                        break;
                    case 3:
                        devcard_text_info.setText(R.string.monopoly_info);
                        break;
                    case 4:
                        devcard_text_info.setText(R.string.road_building_info);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                devcard_text_name.setText(R.string.knight_name);
                devcard_text_info.setText(R.string.knight_info);
            }
        });

        scoreBoardGroup = activity.findViewById(R.id.group_scoreboard);
        this.playerScores = new TextView[]{activity.findViewById(R.id.Player1_Score), activity.findViewById(R.id.Player2_Score), activity.findViewById(R.id.Player3_Score), activity.findViewById(R.id.Player4_Score)};
        this.playerNameTextViews = new TextView[]{activity.findViewById(R.id.Player1_Name), activity.findViewById(R.id.Player2_Name), activity.findViewById(R.id.Player3_Name), activity.findViewById(R.id.Player4_Name)};
        /* -------------------------------------- MENUS ---------------------------------------- */

        /* ------------ Development Card Menu ------------- */

        developmentGroup = activity.findViewById(R.id.group_development_card_menu); // dev card menu GROUP

        useDevCard = activity.findViewById(R.id.use_Card); // use dev card
        useDevCard.setOnClickListener(this);
        Button buildDevCard = activity.findViewById(R.id.build_devCard);
        buildDevCard.setOnClickListener(this);

        /* ---------------- Trade Menu -------------------- */

        tradeGroup = activity.findViewById(R.id.group_trade_menu); // trade menu GROUP
        // confirm and cancel trade buttons
        button_trade_menu_confirm = activity.findViewById(R.id.button_trade_menu_confirm);
        button_trade_menu_confirm.setOnClickListener(this);
        button_trade_menu_cancel = activity.findViewById(R.id.button_trade_menu_cancel);
        button_trade_menu_cancel.setOnClickListener(this);
        tradingWithBankOrPort = activity.findViewById(R.id.trade_with_bank_or_port);
        //Trade Menu Background - Receive
        brickSelectionBoxReceive = activity.findViewById(R.id.brickSelectionBoxReceive);
        grainSelectionBoxReceive = activity.findViewById(R.id.grainSelectionBoxReceive);
        lumberSelectionBoxReceive = activity.findViewById(R.id.lumberSelectionBoxReceive);
        oreSelectionBoxReceive = activity.findViewById(R.id.oreSelectionBoxReceive);
        woolSelectionBoxReceive = activity.findViewById(R.id.woolSelectionBoxReceive);
        //Trade Menu Background - Give
        brickSelectionBoxGive = activity.findViewById(R.id.brickSelectionBoxGive);
        grainSelectionBoxGive = activity.findViewById(R.id.grainSelectiomBoxGive);
        lumberSelectionBoxGive = activity.findViewById(R.id.lumberSelectionBoxGive);
        oreSelectionBoxGive = activity.findViewById(R.id.oreSelectionBoxGive);
        woolSelectionBoxGive = activity.findViewById(R.id.woolSelectionBoxGive);
        //Trade Menu - Receive
        image_trade_menu_give_brick = activity.findViewById(R.id.image_trade_menu_give_brick);
        image_trade_menu_give_brick.setOnClickListener(this);
        image_trade_menu_give_grain = activity.findViewById(R.id.image_trade_menu_give_grain);
        image_trade_menu_give_grain.setOnClickListener(this);
        image_trade_menu_give_lumber = activity.findViewById(R.id.image_trade_menu_give_lumber);
        image_trade_menu_give_lumber.setOnClickListener(this);
        image_trade_menu_give_ore = activity.findViewById(R.id.image_trade_menu_give_ore);
        image_trade_menu_give_ore.setOnClickListener(this);
        image_trade_menu_give_wool = activity.findViewById(R.id.image_trade_menu_give_wool);
        image_trade_menu_give_wool.setOnClickListener(this);
        //Trade Menu - Give
        image_trade_menu_rec_brick = activity.findViewById(R.id.image_trade_menu_rec_brick);
        image_trade_menu_rec_brick.setOnClickListener(this);
        image_trade_menu_rec_grain = activity.findViewById(R.id.image_trade_menu_rec_grain);
        image_trade_menu_rec_grain.setOnClickListener(this);
        image_trade_menu_rec_lumber = activity.findViewById(R.id.image_trade_menu_rec_lumber);
        image_trade_menu_rec_lumber.setOnClickListener(this);
        image_trade_menu_rec_ore = activity.findViewById(R.id.image_trade_menu_rec_ore);
        image_trade_menu_rec_ore.setOnClickListener(this);
        image_trade_menu_rec_wool = activity.findViewById(R.id.image_trade_menu_rec_wool);
        image_trade_menu_rec_wool.setOnClickListener(this);

        ImageView selectionBoxGive[] = {brickSelectionBoxGive, grainSelectionBoxGive, lumberSelectionBoxGive, oreSelectionBoxGive, woolSelectionBoxGive};
        ImageView selectionBoxReceive[] = {brickSelectionBoxReceive, grainSelectionBoxReceive, lumberSelectionBoxReceive, oreSelectionBoxReceive, woolSelectionBoxReceive};

        // set all give selection boxes to transparent
        for (ImageView imageView : selectionBoxGive) {
            imageView.setBackgroundColor(Color.argb(0, 0, 0, 0));
        }
        // set all receive selection boxes to transparent
        for (ImageView imageView : selectionBoxReceive) {
            imageView.setBackgroundColor(Color.argb(0, 0, 0, 0));
        }

        /*--------------------Robber Buttons and Groups------------------------*/

        robberDiscardGroup = activity.findViewById(R.id.robber_discard_group);

        robberBrickPlus = activity.findViewById(R.id.robber_discard_brickAddImg);
        robberBrickMinus = activity.findViewById(R.id.robber_discard_brickMinusImg);
        robberLumberPlus = activity.findViewById(R.id.robber_discard_lumberAddImg);
        robberLumberMinus = activity.findViewById(R.id.robber_discard_lumberMinusImg);
        robberGrainPlus = activity.findViewById(R.id.robber_discard_grainAddImg);
        robberGrainMinus = activity.findViewById(R.id.robber_discard_grainMinusImg);
        robberOrePlus = activity.findViewById(R.id.robber_discard_oreAddImg);
        robberOreMinus = activity.findViewById(R.id.robber_discard_oreMinusImg);
        robberWoolPlus = activity.findViewById(R.id.robber_discard_woolAddImg);
        robberWoolMinus = activity.findViewById(R.id.robber_discard_woolMinusImg);

        robberBrickPlus.setOnClickListener(this);
        robberBrickMinus.setOnClickListener(this);
        robberLumberPlus.setOnClickListener(this);
        robberLumberMinus.setOnClickListener(this);
        robberGrainPlus.setOnClickListener(this);
        robberGrainMinus.setOnClickListener(this);
        robberOrePlus.setOnClickListener(this);
        robberOreMinus.setOnClickListener(this);
        robberWoolPlus.setOnClickListener(this);
        robberWoolMinus.setOnClickListener(this);

        List<String> spinnerList = new ArrayList<>(devCards);
        devCardList.setAdapter(new ArrayAdapter<>(activity, R.layout.support_simple_spinner_dropdown_item, spinnerList));
        messageTextView.setTextColor(Color.WHITE);

        /*--------------------------Monopoly---------------------------------*/

        pickResourceGroup = activity.findViewById(R.id.group_pickResourceMenu);
        pickResourceGroup.setOnClickListener(this);

        monopolyBrickIcon = activity.findViewById(R.id.pickResMenu_brickIcon);
        monopolyBrickIcon.setOnClickListener(this);
        monopolyGrainIcon = activity.findViewById(R.id.pickResMenu_grainIcon);
        monopolyGrainIcon.setOnClickListener(this);
        monopolyLumberIcon = activity.findViewById(R.id.pickResMenu_lumberIcon);
        monopolyLumberIcon.setOnClickListener(this);
        monopolyOreIcon = activity.findViewById(R.id.pickResMenu_oreIcon);
        monopolyOreIcon.setOnClickListener(this);
        monopolyWoolIcon = activity.findViewById(R.id.pickResMenu_woolIcon);
        monopolyWoolIcon.setOnClickListener(this);
        monopolyBrickSelectionBox = activity.findViewById(R.id.pickResMenu_brickSelectionBox);
        monopolyGrainSelectionBox = activity.findViewById(R.id.pickResMenu_grainSelectionBox);
        monopolyLumberSelectionBox = activity.findViewById(R.id.pickResMenu_lumberSelectionBox);
        monopolyOreSelectionBox = activity.findViewById(R.id.pickResMenu_oreSelectionBox);
        monopolyWoolSelectionBox = activity.findViewById(R.id.pickResMenu_woolSelectionBox);

        monopolyConfirm = activity.findViewById(R.id.pickResMenu_ConfirmButton);
        monopolyConfirm.setOnClickListener(this);

        /*----------------------- Trophies ---------------------------------------*/

        armyTrophyImages = new ImageView[]{activity.findViewById(R.id.largest_army_player0), activity.findViewById(R.id.largest_army_player1), activity.findViewById(R.id.largest_army_player2), activity.findViewById(R.id.largest_army_player3)};

        roadTrophyImages = new ImageView[]{activity.findViewById(R.id.longest_road_player0), activity.findViewById(R.id.longest_road_player1), activity.findViewById(R.id.longest_road_player2), activity.findViewById(R.id.longest_road_player3)};

        // Collapse button remains present even with all other actions hidden.
        Button controlsToggle = activity.findViewById(R.id.toggle_sidebar);
        if (controlsToggle != null) {
            controlsToggle.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    controlsCollapsed = !controlsCollapsed;
                    applyControlRailLayout();
                }
            });
        }

        hudLastRoll = activity.findViewById(R.id.hud_last_roll);
        hudHandFirst = activity.findViewById(R.id.hud_hand_first);
        hudHandSecond = activity.findViewById(R.id.hud_hand_second);
        hudCards = activity.findViewById(R.id.hud_development_cards);
        hudLatestAction = activity.findViewById(R.id.hud_latest_action);
        Button historyButton = activity.findViewById(R.id.hud_history_button);
        if (historyButton != null) historyButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override public void onClick(View v) { showGameplayHistory(); }
                });
        // if we have state update the GUI based on the state
        if (this.state != null) receiveInfo(state);
        applyControlRailLayout();
    }// setAsGui() END

    /**
     * The original help topics stay in the XML as source text but are now
     * presented as scrollable, screen-size-aware native Android dialogs.
     */
    private void showHelpTopicsDialog() {
        final String[] topics = {
                "Winning and scoring", "Starting a game", "Building",
                "Development cards", "Trading", "The robber"
        };
        new AlertDialog.Builder(myActivity)
                .setTitle("How to play Catan")
                .setItems(topics, (dialog, which) -> {
                    new AlertDialog.Builder(myActivity)
                            .setTitle(topics[which])
                            .setMessage(helpArticle(which))
                            .setPositiveButton("Close", null)
                            .show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    /** Short, accurate instructions without the legacy help pages' clipping. */
    private String helpArticle(int topic) {
        switch (topic) {
            case 0:
                return "Win by reaching 10 victory points on your own turn.\n\n"
                        + "Settlement: 1 point. City: 2 points (replaces a settlement).\n"
                        + "Longest Road: 2 points for a continuous road of at least 5 segments.\n"
                        + "Largest Army: 2 points once you play at least 3 Knights.\n"
                        + "Victory Point development cards contribute hidden points automatically.";
            case 1:
                return "Start by choosing a settlement on a free intersection, then build its adjoining road.\n\n"
                        + "Each player places twice, with player order reversed in round two. "
                        + "You collect starting resources from the three hexes beside your second settlement.\n\n"
                        + "During normal turns you may play a permitted development card before rolling, "
                        + "then roll, trade, build and end your turn.";
            case 2:
                return "Road: 1 brick + 1 lumber. Join a road or settlement you own.\n\n"
                        + "Settlement: 1 brick + 1 lumber + 1 grain + 1 wool. Must connect to your roads "
                        + "and remain at least two road segments away from any existing settlement or city.\n\n"
                        + "City: 3 ore + 2 grain. Upgrade one of your settlements. "
                        + "It earns 2 victory points and produces double resources.\n\n"
                        + "Your supplies are limited to 15 roads, 5 settlements and 4 cities.";
            case 3:
                return "Purchase a development card with 1 ore + 1 grain + 1 wool.\n\n"
                        + "Knight: move the robber and steal from a player next to its new hex.\n"
                        + "Road Building: place two roads without paying resources.\n"
                        + "Year of Plenty: take any two resources.\n"
                        + "Monopoly: take all cards of one resource from opponents.\n"
                        + "Victory Point: counts automatically towards your total.\n\n"
                        + "You cannot use a newly purchased action card until a later turn, "
                        + "and may play only one ordinary action development card each turn.";
            case 4:
                return "Bank trading: exchange 4 of one resource for 1 of another.\n\n"
                        + "General 3:1 port: settle on a port intersection to exchange 3 of one resource for 1.\n"
                        + "Special 2:1 port: exchange 2 of its named resource for 1 of another.\n\n"
                        + "For a bank trade, clear your selection and tap Trade. For a port, select "
                        + "the port settlement first, then tap Trade.\n\n"
                        + "Note: direct negotiations with computer opponents are still under development.";
            default:
                return "A roll of 7 starts the robber phase. Any player with more than "
                        + "7 resource cards must discard half, rounded down.\n\n"
                        + "Move the robber to a different hexagon. That hex stops producing resources "
                        + "until the robber moves again.\n\n"
                        + "If an opponent has a settlement or city next to the robber, you may steal "
                        + "one random resource card from that player. The robber can also move to an empty hex.";
        }
    }

    /**
     * Spinners, labels and buttons sit inside a native, scrollable dialog.
     * Android sizes it to the landscape window; selections still invoke the
     * existing bank/port trading logic and engine-level validation.
     */
    private void showTradeDialog() {
        if (myActivity == null) return;
        final String[] resources = {"Brick", "Grain", "Lumber", "Ore", "Wool"};
        final boolean viaPort = selectedIntersections.size() == 1;

        LinearLayout contents = new LinearLayout(myActivity);
        contents.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * myActivity.getResources().getDisplayMetrics().density);
        contents.setPadding(pad, pad / 2, pad, pad / 2);

        TextView explanation = new TextView(myActivity);
        explanation.setText(viaPort
                ? "Port trade: choose what to give and receive. Your selected settlement must own this port."
                : "Bank trade: give 4 of one resource to receive 1 of another.");
        explanation.setTextSize(15f);
        contents.addView(explanation);

        TextView giveLabel = new TextView(myActivity);
        giveLabel.setText("You give");
        giveLabel.setPadding(0, pad / 2, 0, 0);
        contents.addView(giveLabel);
        Spinner give = new Spinner(myActivity);
        give.setAdapter(new ArrayAdapter<String>(myActivity,
                android.R.layout.simple_spinner_dropdown_item, resources));
        contents.addView(give);

        TextView receiveLabel = new TextView(myActivity);
        receiveLabel.setText("You receive");
        receiveLabel.setPadding(0, pad / 2, 0, 0);
        contents.addView(receiveLabel);
        Spinner receive = new Spinner(myActivity);
        receive.setAdapter(new ArrayAdapter<String>(myActivity,
                android.R.layout.simple_spinner_dropdown_item, resources));
        receive.setSelection(1);
        contents.addView(receive);

        ScrollView scrolling = new ScrollView(myActivity);
        scrolling.setFillViewport(false);
        scrolling.addView(contents);

        new AlertDialog.Builder(myActivity)
                .setTitle(viaPort ? "Trade with port" : "Trade with bank")
                .setView(scrolling)
                .setPositiveButton("Trade", (dialog, which) -> {
                    int giving = give.getSelectedItemPosition();
                    int receiving = receive.getSelectedItemPosition();
                    boolean accepted = viaPort
                            ? tryTradeWithPort(giving, receiving)
                            : tryTradeWithBank(giving, receiving);
                    if (accepted) {
                        selectedIntersections.clear();
                        tradeGiveSelection = -1;
                        tradeReceiveSelection = -1;
                    } else {
                        Toast.makeText(myActivity, "Trade not available. Check the port and resources.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * The original controls remain functional. Only their visibility and
     * allocated screen width change when the player collapses the control rail.
     */
    private void applyControlRailLayout() {
        if (myActivity == null) return;
        View sidebarGuide = myActivity.findViewById(R.id.guideline_right_sidebar);
        if (sidebarGuide == null) return;
        ConstraintLayout.LayoutParams layout =
                (ConstraintLayout.LayoutParams) sidebarGuide.getLayoutParams();
        layout.guidePercent = controlsCollapsed ? 0.04f : 0.12f;
        sidebarGuide.setLayoutParams(layout);

        int[] hideable = {
                R.id.tabletop_sidebar,
                R.id.sidebar_heading_build,
                R.id.sidebar_heading_build2,
                R.id.sidebar_heading_build3,
                R.id.sidebar_button_road,
                R.id.sidebar_button_settlement,
                R.id.sidebar_button_city,
                R.id.sidebar_button_devcards,
                R.id.sidebar_button_trade,
                R.id.sidebar_button_roll,
                R.id.sidebar_button_endturn,
                R.id.sidebar_button_score,
                R.id.sidebar_button_menu,
                R.id.sidebar_heading_playername,
                R.id.sidebar_heading_vp,
                R.id.sidebar_heading_current_turn,
                R.id.brick_icon, R.id.lumber_icon, R.id.grain_icon,
                R.id.ore_icon, R.id.wool_icon,
                R.id.sidebar_value_brick, R.id.sidebar_value_lumber,
                R.id.sidebar_value_grain, R.id.sidebar_value_ore,
                R.id.sidebar_value_wool,
                R.id.diceImageLeft, R.id.diceImageRight
        };
        for (int id : hideable) {
            View view = myActivity.findViewById(id);
            if (view != null) view.setVisibility(controlsCollapsed ? View.GONE : View.VISIBLE);
        }
        Button toggle = myActivity.findViewById(R.id.toggle_sidebar);
        if (toggle != null) {
            toggle.setText(controlsCollapsed ? "›" : "‹");
            toggle.setContentDescription(controlsCollapsed
                    ? "Expand game controls" : "Collapse game controls");
        }
        if (boardSurfaceView != null) boardSurfaceView.invalidate();
    }

    /**
     *
     */
    private void drawGraphics() {
        Log.d(TAG, "drawGraphics() called");

        if (!this.readyToDraw) {
            Log.e(TAG, "drawGraphics: not ready to draw, not drawing");
            return;
        }

        if (state == null) {
            Log.e(TAG, "drawGraphics: state is null, not drawing");
            return;
        }

        if (state.getBoard() == null) {
            Log.e(TAG, "drawGraphics: board is null returning void and not drawing");
            return;
        }

        boardSurfaceView.setReady(this.readyToDraw);

        boardSurfaceView.setCanvas(new Canvas());

        Log.i(TAG, "drawGraphics: boardSurfaceView height: " + boardSurfaceView.getHeight() + " width: " + boardSurfaceView.getWidth());

        this.boardSurfaceView.setGrid(new HexagonGrid(myActivity.getApplicationContext(), new Board(state.getBoard()), 80, 185, 175, 20, this.debugMode));
        this.boardSurfaceView.draw(boardSurfaceView.getCanvas());

        boardSurfaceView.invalidate();
        initialStateDisplayed = true;
    } // drawGraphics END

    /**
     * Reset the image to gone in case the trophy switched players
     * Sets the visibility of the image view to visible when the player on the trophy
     *
     * @param playerNum - player who hold trophy
     */
    public void showLargestArmyTrophy(int playerNum) {
        Log.d(TAG, "showLargestArmyTrophy() called with: playerNum = [" + playerNum + "]");
        int largestArmyPrevPlayer = state.getCurrentLongestRoadPlayerId();

        for (int i = 0; i < 4; i++) {
            armyTrophyImages[i].setVisibility(View.GONE);

        }
        if (playerNum < 0) {
            Log.w(TAG, "showLongestArmyTrophy: no player has the largest army trophy");
            return;
        }
        armyTrophyImages[playerNum].setVisibility(View.VISIBLE);

        if (largestArmyPrevPlayer == -1) {
            return;
        }
    }

    /**
     * Reset the image to gone in case the trophy switched players
     * Sets the visibility of the image view to visible when the player on the trophy
     *
     * @param playerNum - player who hold trophy
     */
    public void showLongestRoadTrophy(int playerNum) {
        Log.d(TAG, "showLongestRoadTrophy() called with: playerNum = [" + playerNum + "]");
        int LongestRoadPrevPlayer = state.getCurrentLongestRoadPlayerId();

        for (int i = 0; i < 4; i++) {
            Log.i(TAG, "showLongestRoadTrophy: setting to gone");
            roadTrophyImages[i].setVisibility(View.GONE);
        }
        if (playerNum < 0) {
            Log.w(TAG, "showLongestRoadTrophy: no player has the longest road trophy");
            return;
        }
        roadTrophyImages[playerNum].setVisibility(View.VISIBLE);

        if (LongestRoadPrevPlayer == -1) {
            return;
        }

        if (LongestRoadPrevPlayer != playerNum) {
            Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Longest Road Trophy was removed from, " + getAllPlayerNames()[LongestRoadPrevPlayer] + " and was given to, " + getAllPlayerNames()[playerNum], Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
        }
    }

    /**
     * @param message Game over message.
     */
    protected void gameIsOver(String message) {
        Log.d(TAG, "gameIsOver() called with: message = [" + message + "]");
        for (int i = 0; i < state.getPlayerList().size(); i++) {
            int lr = (this.state.getCurrentLongestRoadPlayerId() == i) ? 2 : 0;
            int la = (this.state.getCurrentLargestArmyPlayerId() == i) ? 2 : 0;
            if (this.state.getPlayerList().get(i).getVictoryPointsPrivate() + la + lr + this.state.getPlayerList().get(i).getVictoryPoints() > 9) {
                super.gameIsOver(getAllPlayerNames()[i] + " wins!");
                game.sendAction(new GameOverAckAction(this));
            }
        }
    } // gameIsOver END

    /**
     *
     */
    protected void initAfterReady() {
        Log.e(TAG, "initAfterReady() called");
        this.readyToDraw = true;

        /**
         External Citation
         Date: 2 November 2018
         Problem: Needed to disable the navigation button bar and notification bar; enable fullscreen mode
         Resource: https://developer.android.com/training/system-ui/immersive

         Solution: Remove the visibility of various UI features using the code below
         */
        View decorView = myActivity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        setAsGui(myActivity);
    }

    /**
     * Returns the GUI's top view object
     *
     * @return the top object in the GUI's view hierarchy
     */
    public View getTopView() {
        Log.d(TAG, "getTopView() called");
        return myActivity.findViewById(R.id.top_gui_layout);
    }

    /**
     * Keep the remaining legacy controls (robber, development cards and
     * resource picker) within the visible phone window. ConstraintLayout
     * Groups refer to separate views rather than owning a layout container;
     * each member therefore receives the same centre-and-scale transform.
     * Android's View transforms preserve hit testing for touch controls.
     */
    private void fitPopupOnScreen(final Group group) {
        if (group == null || group.getVisibility() != View.VISIBLE || myActivity == null) return;
        group.post(new Runnable() {
            @Override public void run() {
                if (group.getVisibility() != View.VISIBLE || myActivity == null) return;
                View root = myActivity.findViewById(R.id.top_gui_layout);
                if (root == null || root.getWidth() < 1 || root.getHeight() < 1) return;

                int[] ids = group.getReferencedIds();
                float left = Float.MAX_VALUE, top = Float.MAX_VALUE;
                float right = -Float.MAX_VALUE, bottom = -Float.MAX_VALUE;
                ArrayList<View> members = new ArrayList<>();
                for (int id : ids) {
                    View item = myActivity.findViewById(id);
                    if (item == null || item instanceof android.support.constraint.Guideline
                            || item.getWidth() <= 0 || item.getHeight() <= 0
                            || item.getVisibility() == View.GONE) continue;
                    // Use layout bounds; transformations do not affect getLeft/getTop.
                    item.setScaleX(1f);
                    item.setScaleY(1f);
                    item.setTranslationX(0f);
                    item.setTranslationY(0f);
                    members.add(item);
                    left = Math.min(left, item.getLeft());
                    top = Math.min(top, item.getTop());
                    right = Math.max(right, item.getRight());
                    bottom = Math.max(bottom, item.getBottom());
                }
                if (members.isEmpty() || right <= left || bottom <= top) return;

                float density = myActivity.getResources().getDisplayMetrics().density;
                float inset = Math.max(12f * density, 12f);
                float width = root.getWidth(), height = root.getHeight();
                float usableWidth = Math.max(1f, width - 2f * inset);
                float usableHeight = Math.max(1f, height - 2f * inset);
                float scale = Math.min(1f, Math.min(usableWidth / (right - left),
                        usableHeight / (bottom - top)));
                float originX = (width - (right - left) * scale) / 2f;
                float originY = (height - (bottom - top) * scale) / 2f;
                for (View item : members) {
                    float centerX = (item.getLeft() + item.getRight()) / 2f;
                    float centerY = (item.getTop() + item.getBottom()) / 2f;
                    item.setScaleX(scale);
                    item.setScaleY(scale);
                    item.setTranslationX(originX + (centerX - left) * scale - centerX);
                    item.setTranslationY(originY + (centerY - top) * scale - centerY);
                }
            }
        });
    }

    /**
     * Toggles the visibility of a group.
     *
     * @param group Group to toggle visibility.
     */
    private void toggleGroupVisibilityAllowTapping(Group group) {
        if (group.getVisibility() == View.GONE) {
            group.setVisibility(View.VISIBLE);
            fitPopupOnScreen(group);
        } else group.setVisibility(View.GONE);
    }

    /**
     * Toggles the visibility of a group.
     *
     * @param group Group to toggle visibility.
     */
    private void toggleGroupVisibility(Group group) {
        if (group.getVisibility() == View.GONE) {
            this.isMenuOpen = true;
            group.setVisibility(View.VISIBLE);
            fitPopupOnScreen(group);
        } else {
            this.isMenuOpen = false;
            group.setVisibility(View.GONE);
        }
    }

    private void toggleGroupVisibilityGONE(Group group) {
        group.setVisibility(View.GONE);
    }

    private void toggleGroupVisibilityVISIBLE(Group group) {
        group.setVisibility(View.VISIBLE);
        fitPopupOnScreen(group);
    }


    /**
     * @param view View to toggle.
     */
    private void toggleViewVisibility(View view) {
        if (view.getVisibility() == View.GONE) view.setVisibility(View.VISIBLE);
        else view.setVisibility(View.GONE);
    }

    /**
     * Sets all buttons to visible and clickable.
     */
    private void setAllButtonsToVisible() {
        this.buildRoadButton.setAlpha(1f);
        this.buildRoadButton.setClickable(true);
        this.buildSettlementButton.setAlpha(1f);
        this.buildSettlementButton.setClickable(true);
        this.buildCityButton.setAlpha(1f);
        this.buildCityButton.setClickable(true);
        this.endTurnButton.setAlpha(1f);
        this.endTurnButton.setClickable(true);
        this.sidebarOpenDevCardMenuButton.setAlpha(1f);
        this.sidebarOpenDevCardMenuButton.setClickable(true);
        this.tradeButton.setAlpha(1f);
        this.tradeButton.setClickable(true);
        this.buildSettlementButton.setAlpha(1f);
        this.buildSettlementButton.setClickable(true);
        this.buildRoadButton.setAlpha(1f);
        this.buildRoadButton.setClickable(true);
        this.endTurnButton.setAlpha(1f);
        this.endTurnButton.setClickable(true);
        this.rollButton.setAlpha(1f);
        this.rollButton.setClickable(true);
    }

    /**
     *
     */
    private void hideAllMenusAtEndOfTurn() {
        developmentGroup.setVisibility(View.GONE);
        tradeGroup.setVisibility(View.GONE);
    }

    /**
     * Make a View Blink for a desired duration
     *
     * @param view View to be animated.
     * @return returns The View with animation properties on it.
     */
    private static View blinkAnimation(View view) {
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(300);
        anim.setStartOffset(100);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(3);
        view.startAnimation(anim);
        return view;
    }

    /**
     * @param v View to make shake.
     */
    private void shake(TextView v) {
        Animation shake = AnimationUtils.loadAnimation(myActivity.getApplicationContext(), R.anim.shake_anim);
        v.setTextColor(Color.RED);
        v.startAnimation(shake);
        v.startAnimation(shake);
    }

    /**
     * @return names of all the players in the game
     */
    private String[] getAllPlayerNames() {
        return super.allPlayerNames;
    }

    /**
     * Invoked whenever the player's timer has ticked. It is expected
     * that this will be overridden in many games.
     */
    @Override
    protected void timerTicked() {
        // by default, do nothing
        Log.e(TAG, "timerTicked: timer ticked");
    }

}// class CatanHumanPlayer END

