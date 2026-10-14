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
import edu.up.cs.androidcatan.catan.actions.CatanTradeWithAutoBankAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeOfferAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeReplyAction;
import edu.up.cs.androidcatan.catan.actions.CatanTradeChooseAction;
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
    // Building is asynchronous: success must come from the authoritative
    // board update, never from merely queuing an action.
    private static final int PENDING_NONE = 0;
    private static final int PENDING_SETTLEMENT = 1;
    private static final int PENDING_CITY = 2;
    private static final int PENDING_ROAD = 3;
    private int pendingBuild = PENDING_NONE;
    private int pendingIntersection = -1;
    private int pendingRoadFrom = -1;
    private int pendingRoadTo = -1;


    // resourceCard index values: 0 = Brick, 1 = Lumber, 2 = Grain, 3 = Ore, 4 = Wool
    private int[] robberDiscardedResources = new int[]{0, 0, 0, 0, 0};  //How many resources the player would like to discard
    private int selectedDevCard = -1;
    private int selectedResourceId = -1;
    private int firstYearOfPlentyResource = -1;
    private TextView messageTextView = (TextView) null;

    private ArrayList<String> devCards = new ArrayList<>();
    private boolean developmentPurchasePending = false;
    private ArrayList<Integer> developmentHandBeforePurchase = null;

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
    private boolean activityHistoryLoaded;
     private int[] lastOwnResources;
    private static final String[] HAND_RESOURCES = {"Brick", "Grain", "Lumber", "Ore", "Wool"};
    private volatile boolean initialStateDisplayed;

    /** True only once the initial game state has populated the board display. */
    public boolean isInitialStateDisplayed() {
        return initialStateDisplayed && boardSurfaceView != null && state != null;
    }
    private boolean controlsCollapsed = false;
    private boolean hudExpanded = false;
    // In-memory quick repeat; a new match never inherits an old draft.
    private int[] previousPlayerTradeGive;
    private int[] previousPlayerTradeWant;




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
            if (pendingBuild != PENDING_NONE) {
                messageTextView.setText("Wait for the previous build to finish.");
                return;
            }
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
            if (pendingBuild != PENDING_NONE) {
                messageTextView.setText("Wait for the previous build to finish.");
                return;
            }
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
                // A valid local preview only queues a build. The game may
                // reject it, so do not show "Built" or clear the highlight yet.
                tryBuildSettlement(selectedIntersections.get(0));
            }
            return;
        }

        // City button on the sidebar.
        if (button.getId() == R.id.sidebar_button_city) {
            if (pendingBuild != PENDING_NONE) {
                messageTextView.setText("Wait for the previous build to finish.");
                return;
            }
            //Make sure an intersection has been selected to build on(Just 1)
            if (selectedIntersections.size() != 1) {
                messageTextView.setText(R.string.select_one_int_for_city);
                Toast toast = Toast.makeText(myActivity.getApplicationContext(), "Select one intersection to build a city.", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();

            } else {
                Log.e(TAG, "onClick: build city selected intersection: " + selectedIntersections.get(0));

                //Build the city
                tryBuildCity(selectedIntersections.get(0));
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
        if (button.getId() == R.id.sidebar_button_score) {
            if (state.getPlayerList().size() != 4) showAllPlayersScoreboard();
            else toggleGroupVisibilityAllowTapping(scoreBoardGroup);
            return;
        }

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

        // An illustrated, scrollable private hand replaces the old cramped dropdown.
        if (button.getId() == R.id.sidebar_button_devcards) {
            showDevelopmentCollection();
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
            buyDevelopmentCard();
            return;
        }

        /* ------------------------------------ Trading ---------------------------------- */

        // A single responsive trade dialog replaces the rigid 26-view overlay.
        if (button.getId() == R.id.sidebar_button_trade) {
            // Harbour access is detected automatically. Never require the
            // player to choose a board intersection just to trade.
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

                if (CatanExperienceSettings.music(myActivity)) {
                    menuMusic.setLooping(false);
                    menuMusic.setVolume(1f, 1f);
                    menuMusic.seekTo(0);
                    generalMusic.setVolume(0, 0);
                    menuMusic.start();
                }
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
                    if (menuMusic != null && menuMusic.isPlaying())
                        menuMusic.pause();
                    if (generalMusic != null)
                        generalMusic.setVolume(
                                CatanExperienceSettings.music(myActivity) ? 1f : 0f,
                                CatanExperienceSettings.music(myActivity) ? 1f : 0f);
                }
                break;
            }
        }

    } // onClick END

    /* ----------------------- BoardSurfaceView Touch Listeners --------------------------------- */

    // Tap selects one snapped junction; dragging moves the map, pinching zooms.
    // One touch sequence must never both pan and accidentally place a building.
    private float boardTouchX, boardTouchY, boardDownX, boardDownY, boardPinchSpan;
    private boolean boardGestureMoved;
    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (boardSurfaceView == null) return false;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                boardTouchX = boardDownX = event.getX();
                boardTouchY = boardDownY = event.getY();
                boardGestureMoved = false;
                boardPinchSpan = 0f;
                return true;
            }
            if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() >= 2) {
                boardGestureMoved = true;
                boardPinchSpan = (float) Math.hypot(
                        event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (event.getPointerCount() >= 2) {
                    float newSpan = (float) Math.hypot(
                            event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
                    if (boardPinchSpan > 0f && newSpan > 0f) {
                        float cx = (event.getX(0) + event.getX(1)) * 0.5f;
                        float cy = (event.getY(0) + event.getY(1)) * 0.5f;
                        boardSurfaceView.zoomBy(newSpan / boardPinchSpan, cx, cy);
                    }
                    boardPinchSpan = newSpan;
                    boardGestureMoved = true;
                } else {
                    float dx = event.getX() - boardTouchX;
                    float dy = event.getY() - boardTouchY;
                    float totalDx = event.getX() - boardDownX;
                    float totalDy = event.getY() - boardDownY;
                    if (boardGestureMoved || totalDx * totalDx + totalDy * totalDy > 144f) {
                        boardGestureMoved = true;
                        boardSurfaceView.panBy(dx, dy);
                    }
                    boardTouchX = event.getX();
                    boardTouchY = event.getY();
                }
                return true;
            }
            if (action == MotionEvent.ACTION_POINTER_UP) {
                int surviving = event.getActionIndex() == 0 ? 1 : 0;
                if (surviving < event.getPointerCount()) {
                    boardTouchX = event.getX(surviving);
                    boardTouchY = event.getY(surviving);
                }
                boardPinchSpan = 0f;
                boardGestureMoved = true;
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                if (!boardGestureMoved && !isMenuOpen && state != null
                        && playerNum == state.getCurrentPlayerId()) {
                    lastTouchDownXY[0] = boardSurfaceView.toBoardX(event.getX());
                    lastTouchDownXY[1] = boardSurfaceView.toBoardY(event.getY());
                    v.performClick();
                }
                return true;
            }
            return action != MotionEvent.ACTION_CANCEL;
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

            // Only ONE junction may be selected for each tap. Previously the
            // 200x200 hit boxes overlapped and a single tap could toggle two
            // junctions, making the chosen build position look arbitrary.
            int nearest = -1;
            float nearestDistanceSquared = 88f * 88f;
            for (int i = 0; i < grid.getIntersections().length; i++) {
                float dx = x - grid.getIntersections()[i].getXPos();
                float dy = y - grid.getIntersections()[i].getYPos();
                float distanceSquared = dx * dx + dy * dy;
                if (distanceSquared < nearestDistanceSquared) {
                    nearestDistanceSquared = distanceSquared;
                    nearest = i;
                }
            }
            if (nearest >= 0) {
                touchedIntersection = true;
                if (grid.getHighlightedIntersections().contains(nearest)) {
                    grid.getHighlightedIntersections().remove((Integer) nearest);
                    selectedIntersections.remove((Integer) nearest);
                } else {
                    // The road tool permits up to two selected junctions.
                    // Keep their visible highlights and internal IDs in sync.
                    if (selectedIntersections.size() >= 2) {
                        Integer old = selectedIntersections.remove(0);
                        grid.getHighlightedIntersections().remove(old);
                    }
                    selectedIntersections.add(nearest);
                    grid.getHighlightedIntersections().remove((Integer) nearest);
                    grid.addHighlightedIntersection(nearest);
                }
                grid.setHighlightedHexagon(-1);
                selectedHexagonId = -1;
                if (!selectedIntersections.isEmpty()) {
                    String chosen = selectedIntersections.size() == 1
                            ? "Junction " + selectedIntersections.get(0) + " selected"
                            : "Junctions " + selectedIntersections.get(0) + " and "
                                    + selectedIntersections.get(1) + " selected";
                    messageTextView.setText(chosen + " — tap Build to confirm.");
                }
                boardSurfaceView.invalidate();
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
        if (pendingBuild != PENDING_NONE) return false;
        if (state.getCurrentPlayerId() != playerNum || state.isRobberPhase()) {
            showBuildProblem("Wait for your turn and finish the robber.");
            return false;
        }
        if (!state.getBoard().validRoadPlacement(playerNum, state.isSetupPhase(),
                intersectionA, intersectionB,
                state.isSetupPhase() ? state.getCurrentSetupSettlement() : -1)) {
            showBuildProblem("That road is not a free edge connected to your network.");
            return false;
        }
        boolean free = !state.isSetupPhase() && state.getFreeRoadsRemaining() > 0;
        if (!state.isSetupPhase() && !state.isActionPhase() && !free) {
            showBuildProblem("Roll the dice before building a road.");
            return false;
        }
        if (!state.isSetupPhase() && !free
                && !state.getCurrentPlayer().hasResourceBundle(Road.resourceCost)) {
            showBuildProblem("Road needs 1 brick and 1 lumber.");
            return false;
        }
        pendingBuild = PENDING_ROAD;
        pendingRoadFrom = intersectionA;
        pendingRoadTo = intersectionB;
        messageTextView.setText("Building road — checking the game...");
        game.sendAction(new CatanBuildRoadAction(this, state.isSetupPhase(),
                playerNum, intersectionA, intersectionB));
        return true;
    }

    /**
     * @param intersection1 IntersectionDrawable at which the player is trying to build a settlement upon.
     * @return If the building location chosen is valid, and if the action was carried out.
     */
    private String settlementProblem(int point) {
        if (state == null || playerNum != state.getCurrentPlayerId())
            return "It is not your turn.";
        if (state.isRobberPhase())
            return "Finish the robber action first.";
        if (!state.isSetupPhase() && !state.isActionPhase())
            return "Roll the dice before building a settlement.";
        if (state.isSetupPhase() && state.getCurrentSetupSettlement() >= 0)
            return "Build the road beside your opening settlement first.";
        if (!state.isSetupPhase()
                && !state.getCurrentPlayer().hasResourceBundle(
                        edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement.resourceCost))
            return "Settlement needs 1 brick, 1 grain, 1 lumber and 1 wool.";
        if (point < 0 || point >= state.getBoard().getBuildings().length)
            return "Select a junction on the island.";
        if (state.getBoard().getBuildingAtIntersection(point) != null)
            return "There is already a settlement or city at this junction.";
        for (Integer neighbour : state.getBoard().getIntersectionAdjacencyList().get(point)) {
            if (state.getBoard().getBuildingAtIntersection(neighbour) != null)
                return "Too close to another settlement or city (one empty junction is required).";
        }
        if (!state.isSetupPhase() && !state.getBoard().isConnected(playerNum, point))
            return "Connect this junction to one of your own roads first.";
        int settlements = 0;
        for (edu.up.cs.androidcatan.catan.gamestate.buildings.Building building
                : state.getBoard().getBuildings()) {
            if (building instanceof edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement
                    && building.getOwnerId() == playerNum) settlements++;
        }
        if (settlements >= 5) return "All five settlements are in use. Upgrade one to a city first.";
        if (!state.getBoard().validBuildingLocation(playerNum, state.isSetupPhase(), point))
            return "That junction is not valid under the settlement distance rule.";
        return null;
    }

    private void showBuildProblem(String reason) {
        if (messageTextView != null) messageTextView.setText(reason);
        if (myActivity != null)
            Toast.makeText(myActivity, reason, Toast.LENGTH_SHORT).show();
    }

    private boolean tryBuildSettlement(int intersection1) {
        if (pendingBuild != PENDING_NONE) return false;
        String problem = settlementProblem(intersection1);
        if (problem != null) {
            showBuildProblem(problem);
            return false;
        }
        pendingBuild = PENDING_SETTLEMENT;
        pendingIntersection = intersection1;
        messageTextView.setText("Building settlement — checking the game...");
        game.sendAction(new CatanBuildSettlementAction(this,
                state.isSetupPhase(), playerNum, intersection1));
        return true;
    }

    /**
     * @param intersection Intersection player is attempting to build a city at.
     * @return If a city was built at the intersection.
     */
    private boolean tryBuildCity(int intersection) {
        if (pendingBuild != PENDING_NONE) return false;
        if (state.getCurrentPlayerId() != playerNum || state.isRobberPhase()
                || state.isSetupPhase() || !state.isActionPhase()) {
            showBuildProblem("You can upgrade a city only on your normal build turn.");
            return false;
        }
        if (!state.getCurrentPlayer().hasResourceBundle(City.resourceCost)) {
            showBuildProblem("City needs 2 grain and 3 ore.");
            return false;
        }
        if (!state.getBoard().validCityLocation(playerNum, intersection)) {
            showBuildProblem("Select one of your existing settlements to upgrade.");
            return false;
        }
        pendingBuild = PENDING_CITY;
        pendingIntersection = intersection;
        messageTextView.setText("Upgrading city — checking the game...");
        game.sendAction(new CatanBuildCityAction(this,
                state.isSetupPhase(), playerNum, intersection));
        return true;
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

    /** Viewable at any time; actual buying and playing remains turn-restricted. */
    private void showDevelopmentCollection() {
        if (state == null || myActivity == null) return;
        CatanDevelopmentCardsUI.showCollection(myActivity, state, playerNum,
                developmentPurchasePending, new CatanDevelopmentCardsUI.Listener() {
                    @Override public void buyCard() { buyDevelopmentCard(); }
                    @Override public void playCard(int cardId) { playDevelopmentCard(cardId); }
                });
    }

    private void buyDevelopmentCard() {
        if (state == null || developmentPurchasePending) return;
        if (state.getCurrentPlayerId() != playerNum || state.isSetupPhase()
                || state.isRobberPhase() || !state.isActionPhase()) {
            showBuildProblem("You can buy development cards only after rolling on your turn.");
            return;
        }
        if (!state.hasDevelopmentCardsAvailable()) {
            showBuildProblem("The development deck is empty.");
            return;
        }
        if (!state.getPlayerList().get(playerNum).hasResourceBundle(DevelopmentCard.resourceCost)) {
            showBuildProblem("You need 1 wool, 1 grain and 1 ore to buy a development card.");
            return;
        }
        developmentPurchasePending = true;
        developmentHandBeforePurchase =
                new ArrayList<>(state.getPlayerList().get(playerNum).getDevelopmentCards());
        messageTextView.setText("Drawing a random development card...");
        game.sendAction(new CatanBuyDevCardAction(this));
    }

    /**
     * The new card's type is disclosed only to the human buying it and only
     * after an accepted state update has placed it in the private hand.
     */
    private int findNewDevelopmentCard(CatanGameState next) {
        if (!developmentPurchasePending || developmentHandBeforePurchase == null
                || next == null) return -1;
        ArrayList<Integer> added = new ArrayList<>(
                next.getPlayerList().get(playerNum).getDevelopmentCards());
        for (Integer previous : developmentHandBeforePurchase)
            added.remove(previous); // Removes one copy, preserving duplicate card counts.
        if (added.isEmpty()) return -1;
        developmentPurchasePending = false;
        developmentHandBeforePurchase = null;
        return added.get(0);
    }

    private void rejectDevelopmentPurchase(boolean notYourTurn) {
        developmentPurchasePending = false;
        developmentHandBeforePurchase = null;
        showBuildProblem(notYourTurn ? "Wait for your turn to buy development cards."
                : "The game could not complete that purchase; no card was added.");
    }

    private void playDevelopmentCard(int id) {
        if (state == null || id < 0 || id > 4
                || state.getCurrentPlayerId() != playerNum || state.isSetupPhase()
                || state.isRobberPhase() || state.hasPlayedDevelopmentCardThisTurn()
                || !state.getPlayerList().get(playerNum).getPlayableDevCards().contains(id)) {
            showBuildProblem("That card cannot be played yet. New cards become usable next turn.");
            return;
        }
        if (id == 1) {
            messageTextView.setText("Victory Point cards count automatically and stay secret.");
            return;
        }
        if (id == 0) {
            game.sendAction(new CatanUseKnightCardAction(this));
        } else if (id == 2 || id == 3) {
            if (pickResourceGroup != null && pickResourceGroup.getVisibility() != View.VISIBLE)
                toggleGroupVisibility(pickResourceGroup);
            selectedDevCard = id;
            firstYearOfPlentyResource = -1;
            selectedResourceId = -1;
            messageTextView.setText(id == 2
                    ? "Select your first of two Year of Plenty resources."
                    : "Select a resource for Monopoly.");
        } else if (id == 4) {
            game.sendAction(new CatanUseRoadBuildingCardAction(this));
            messageTextView.setText("Road Building played — place your two free roads.");
        }
    }

    /* ---------------------------------------- GUI Methods --------------------------------------*/

    /**
     * updates all text views a gui components to reflect current game state
     */
    /** Always-visible match details, even when the action rail is collapsed. */
    private void updateGameplayHud() {
        if (myActivity == null || state == null || playerNum < 0
                || playerNum >= state.getPlayerList().size()) return;
        // Small persistent indicator only; never cover the island or dice.
        if (tradeButton != null) {
            CatanMarketState.Event event = state.getMarketState().getActiveEvent();
            tradeButton.setText(event == null ? "Trade" : "Trade ✦");
            tradeButton.setContentDescription(event == null ? "Trade resources"
                    : "Trade resources. Active market event: " + event.title);
        }
        int[] hand = state.getPlayerList().get(playerNum).getResourceCards();
        if (hudHandFirst != null)
            hudHandFirst.setText("YOUR HAND · Brick " + hand[0]
                    + " · Grain " + hand[1] + " · Lumber " + hand[2]);
        if (hudHandSecond != null)
            hudHandSecond.setText("Ore " + hand[3] + " · Wool " + hand[4]
                    + " · Total " + state.getPlayerList().get(playerNum)
                            .getTotalResourceCardCount());

        if (hudLastRoll != null) {
            String namedRoll = CatanMatchHistory.lastRoll(myActivity);
            if (!namedRoll.isEmpty()) {
                // Keep the compact HUD to the dice; full production remains in History.
                int detail = namedRoll.indexOf(" — production:");
                if (detail < 0) detail = namedRoll.indexOf(" — no resources produced");
                hudLastRoll.setText(detail >= 0 ? namedRoll.substring(0, detail) : namedRoll);
            } else if (state.isSetupPhase()) {
                hudLastRoll.setText("Dice: opening placements");
            } else if (state.isActionPhase()) {
                int[] dice = state.getDice().getDiceValues();
                hudLastRoll.setText("Current dice: " + dice[0] + " + "
                        + dice[1] + " = " + state.getCurrentDiceSum());
            } else {
                hudLastRoll.setText("Dice: waiting for roll");
            }
        }

        if (hudCards != null) {
            int[] counts = new int[HUD_CARD_NAMES.length];
            for (Integer card : state.getPlayerList().get(playerNum).getDevelopmentCards())
                if (card != null && card >= 0 && card < counts.length) counts[card]++;
            StringBuilder summary = new StringBuilder("Your development cards: ");
            int types = 0;
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] == 0) continue;
                if (types++ > 0) summary.append(" · ");
                summary.append(HUD_CARD_NAMES[i]).append(" ×").append(counts[i]);
            }
            if (types == 0) summary.append("None");
            hudCards.setText(summary.toString());
        }
        if (hudLatestAction != null)
            hudLatestAction.setText("Latest: " + CatanMatchHistory.latest(myActivity));
    }

    /** Full scoreboard for optional matches with 2–7 total players. */
    private void showAllPlayersScoreboard() {
        if (state == null || myActivity == null) return;
        StringBuilder details = new StringBuilder();
        for (int i = 0; i < state.getPlayerList().size(); i++) {
            Player contender = state.getPlayerList().get(i);
            int bonus = (state.getCurrentLargestArmyPlayerId() == i ? 2 : 0)
                    + (state.getCurrentLongestRoadPlayerId() == i ? 2 : 0);
            int points = contender.getVictoryPoints() + bonus;
            if (i == playerNum) points += contender.getVictoryPointsPrivate();
            details.append(i == state.getCurrentPlayerId() ? "➤ " : "  ");
            details.append(allPlayerNames[i]).append(" — ").append(points);
            details.append(" VP");
            if (i == playerNum) details.append(" (your private total)");
            details.append('\n');
        }
        new AlertDialog.Builder(myActivity)
                .setTitle("Scoreboard · " + state.getPlayerList().size() + " players")
                .setMessage(details.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void showGameplayHistory() {
        if (myActivity == null) return;
        String history = CatanMatchHistory.history(myActivity);
        String[] lines = history.isEmpty() ? new String[0] : history.split("\\n");
        StringBuilder report = new StringBuilder();
        if (state != null && allPlayerNames != null) {
            report.append("OTHER PLAYERS — PUBLIC CARD COUNTS");
            for (int i = 0; i < state.getPlayerList().size(); i++) {
                if (i == playerNum) continue;
                Player opponent = state.getPlayerList().get(i);
                report.append('\n').append(allPlayerNames[i]).append(": ")
                        .append(opponent.getTotalResourceCardCount())
                        .append(" resource cards · ")
                        .append(opponent.getDevelopmentCards().size())
                        .append(" development cards");
            }
            report.append('\n').append('\n');
        }
        if (lines.length == 0) report.append("No moves recorded yet.");
        for (int i = lines.length - 1; i >= 0; i--) {
            if (!lines[i].trim().isEmpty())
                report.append("• ").append(lines[i]).append('\n').append('\n');
        }
        new AlertDialog.Builder(myActivity)
                .setTitle("Rolls & actions — newest first")
                .setMessage(report.toString())
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
            // Classic four-player snake order: 0,1,2,3,3,2,1,0.
            // Spell out that the human must wait for a SECOND free placement.
            final int placementNumber = state.getSetupPhaseTurnCounter() + 1;
            final boolean secondRound = placementNumber > state.getPlayerList().size();
            final boolean isMyPlacement = state.getCurrentPlayerId() == playerNum;
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

            if (!isMyPlacement) {
                String computerName = allPlayerNames[state.getCurrentPlayerId()];
                this.messageTextView.setText("Opening placement " + placementNumber
                        + " of " + state.getOpeningPlacementCount() + " — " + computerName
                        + " is placing. Your second free settlement comes after the computers.");
            } else if (settlements == 0) {
                this.messageTextView.setText(secondRound
                        ? "Your SECOND opening settlement (2 of 2): select an empty junction and tap Settlement. No resource cost."
                        : "Your FIRST opening settlement (1 of 2): select an empty junction and tap Settlement. No resource cost.");
            } else if (roads == 0) {
                this.messageTextView.setText(secondRound
                        ? "Now place the free road touching your SECOND settlement."
                        : "Now place the free road touching your FIRST settlement.");
            } else {
                this.messageTextView.setText(secondRound
                        ? "Second opening settlement and road complete. Tap END TURN to start the game."
                        : "First opening settlement and road complete. Tap END TURN; the computer players go next, then you place your SECOND pair.");
            }

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

        // The collection is always inspectable, even during the CPUs' turns
        // or opening placements. Individual Buy/Play controls enforce rules.
        this.sidebarOpenDevCardMenuButton.setAlpha(1f);
        this.sidebarOpenDevCardMenuButton.setClickable(true);

        // Clear turn indicator remains visible even when controls collapse.
        TextView turnChip = myActivity.findViewById(R.id.turn_status_chip);
        if (turnChip != null) {
            String who = getAllPlayerNames()[state.getCurrentPlayerId()];
            String stage = state.isSetupPhase()
                    ? "Opening " + (state.getSetupPhaseTurnCounter() + 1)
                        + "/" + state.getOpeningPlacementCount()
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

        /* ----- update scoreboard ----- */

        // set the other players score on the scoreboard to their public scores except for the user which shows their private score



        for (int i = 0; i < Math.min(4, state.getPlayerList().size()); i++) {
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
            if (i >= state.getPlayerList().size()) {
                this.playerNameTextViews[i].setText("");
                this.playerScores[i].setText("");
                this.playerNameTextViews[i].setBackgroundColor(Color.TRANSPARENT);
                this.playerScores[i].setBackgroundColor(Color.TRANSPARENT);
                continue;
            }
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

    /**
     * Display the one most recent confirmed construction or robber action.
     * Compare copied authoritative states rather than responding to unapproved
     * button taps, so rejected actions never animate.
     */
    private void showBoardProgress(CatanGameState before, CatanGameState after) {
        if (before == null || after == null || boardSurfaceView == null
                || myActivity == null
                || !CatanExperienceSettings.animations(myActivity)) return;
        edu.up.cs.androidcatan.catan.gamestate.buildings.Building[] earlier =
                before.getBoard().getBuildings();
        edu.up.cs.androidcatan.catan.gamestate.buildings.Building[] updated =
                after.getBoard().getBuildings();
        for (int junction = 0; junction < Math.min(earlier.length, updated.length); junction++) {
            if (updated[junction] == null) continue;
            boolean newBuilding = earlier[junction] == null;
            boolean cityUpgrade = updated[junction] instanceof City
                    && !(earlier[junction] instanceof City);
            if (newBuilding || cityUpgrade) {
                boardSurfaceView.flashIntersection(junction);
                CatanExperienceSounds.play(myActivity, CatanExperienceSounds.Cue.BUILD);
                return;
            }
        }
        if (after.getBoard().getRoads().size() > before.getBoard().getRoads().size()) {
            for (Road road : after.getBoard().getRoads()) {
                boolean found = false;
                for (Road previous : before.getBoard().getRoads()) {
                    if (previous.getIntersectionAId() == road.getIntersectionAId()
                            && previous.getIntersectionBId() == road.getIntersectionBId()
                            && previous.getOwnerId() == road.getOwnerId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    boardSurfaceView.flashRoad(road.getIntersectionAId(),
                            road.getIntersectionBId());
                    CatanExperienceSounds.play(myActivity, CatanExperienceSounds.Cue.BUILD);
                    return;
                }
            }
        }
        if (before.getBoard().getRobber().getHexagonId()
                != after.getBoard().getRobber().getHexagonId())
            boardSurfaceView.flashHex(after.getBoard().getRobber().getHexagonId());
    }

    /** Show exactly how the human's own resource hand changed after a move. */
    private void captureOwnResourceChanges() {
        int[] next = state.getPlayerList().get(playerNum).getResourceCards();
        if (lastOwnResources != null && myActivity != null) {
            StringBuilder change = new StringBuilder();
            for (int resource = 0; resource < 5; resource++) {
                int delta = next[resource] - lastOwnResources[resource];
                if (delta == 0) continue;
                if (change.length() > 0) change.append(", ");
                change.append(delta > 0 ? "+" : "").append(delta)
                        .append(" ").append(HAND_RESOURCES[resource]);
            }
            if (change.length() > 0) {
                CatanMatchHistory.append(myActivity, "Your resources: " + change);
                CatanExperienceFeedback.notice(myActivity, "Your resources: " + change, false);
            }
        }
        lastOwnResources = next.clone();
    }

    /**
     * A build is confirmed only if the next authoritative board actually
     * contains the requested piece. No premature success on sendAction().
     */
    private String confirmPendingBuild(CatanGameState next) {
        if (pendingBuild == PENDING_NONE || next == null) return null;
        String confirmation = null;
        if (pendingBuild == PENDING_SETTLEMENT) {
            edu.up.cs.androidcatan.catan.gamestate.buildings.Building piece =
                    next.getBoard().getBuildingAtIntersection(pendingIntersection);
            if (piece instanceof edu.up.cs.androidcatan.catan.gamestate.buildings.Settlement
                    && piece.getOwnerId() == playerNum)
                confirmation = next.isSetupPhase()
                        ? "Opening settlement " + (next.getSetupPhaseTurnCounter() >= next.getPlayerList().size() ? "2" : "1")
                            + " of 2 placed. Now select the adjoining road."
                        : "Settlement built at junction " + pendingIntersection + ".";
        } else if (pendingBuild == PENDING_CITY) {
            edu.up.cs.androidcatan.catan.gamestate.buildings.Building piece =
                    next.getBoard().getBuildingAtIntersection(pendingIntersection);
            if (piece instanceof City && piece.getOwnerId() == playerNum)
                confirmation = "City upgraded at junction " + pendingIntersection + ".";
        } else if (pendingBuild == PENDING_ROAD) {
            for (Road road : next.getBoard().getRoads()) {
                if (road.getOwnerId() == playerNum
                        && ((road.getIntersectionAId() == pendingRoadFrom
                                    && road.getIntersectionBId() == pendingRoadTo)
                            || (road.getIntersectionAId() == pendingRoadTo
                                    && road.getIntersectionBId() == pendingRoadFrom))) {
                    confirmation = next.isSetupPhase()
                            ? next.getSetupPhaseTurnCounter() >= next.getPlayerList().size()
                                ? "Second starting pair complete! Tap END TURN to start the match."
                                : "First starting pair complete! Tap END TURN. The computers place their pairs, then you place your second."
                            : "Road built between junctions "
                                + pendingRoadFrom + " and " + pendingRoadTo + ".";
                    break;
                }
            }
        }
        if (confirmation != null) {
            pendingBuild = PENDING_NONE;
            pendingIntersection = -1;
            pendingRoadFrom = pendingRoadTo = -1;
            selectedIntersections.clear();
            if (boardSurfaceView != null && boardSurfaceView.getGrid() != null)
                boardSurfaceView.getGrid().clearHighLightedIntersections();
        }
        return confirmation;
    }

    private void rejectPendingBuild(boolean notYourTurn) {
        if (pendingBuild == PENDING_NONE) {
            showBuildProblem(notYourTurn
                    ? "Wait for your turn before making a move."
                    : "Move not accepted. Check the selected position and game rules.");
            return;
        }
        String reason = "The game rejected that build. Your resources have not been spent.";
        if (notYourTurn) reason = "This is not your turn to build.";
        else if (pendingBuild == PENDING_SETTLEMENT) {
            String details = settlementProblem(pendingIntersection);
            if (details != null) reason = details;
        }
        pendingBuild = PENDING_NONE;
        pendingIntersection = -1;
        pendingRoadFrom = pendingRoadTo = -1;
        // Keep the selected junction highlighted, so it can be corrected.
        showBuildProblem(reason);
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

        if (info instanceof CatanTradeInfo) {
            CatanTradeInfo trade = (CatanTradeInfo) info;
            if (myActivity != null) myActivity.runOnUiThread(() -> {
                if (trade.getKind() == CatanTradeInfo.REQUEST
                        && trade.getProposerId() != playerNum) showIncomingTradeOffer(trade);
                else if (trade.getKind() == CatanTradeInfo.RESULTS
                        && trade.getProposerId() == playerNum)
                    showTradeComparisons(CatanTradeResponsesInfo.fromTradeInfo(trade));
                else if (trade.getKind() == CatanTradeInfo.CANCELLED
                        || trade.getKind() == CatanTradeInfo.COMPLETE)
                    Toast.makeText(myActivity, trade.getMessage(), Toast.LENGTH_SHORT).show();
            });
            return;
        }
        if (info instanceof CatanPublicEventInfo) {
            CatanPublicEventInfo notice = (CatanPublicEventInfo) info;
            if (myActivity != null) {
                CatanMatchHistory.append(myActivity, notice.getDescription());
                if (CatanExperienceFeedback.isImportant(notice.getDescription()))
                    CatanExperienceFeedback.notice(myActivity, notice.getDescription(), true);
            }
            updateGameplayHud();
            if (notice.isDiceRoll() && myActivity != null) {
                String last = CatanMatchHistory.lastRoll(myActivity);
                int detail = last.indexOf(" — ");
                if (detail >= 0) last = last.substring(0, detail);
                CatanExperienceFeedback.dice(myActivity, hudLastRoll, last);
            }
            return; // A public notice must not clear selected roads or intersections.
        }
        if (this.boardSurfaceView == null) {
            Log.e(TAG, "receiveInfo: boardSurfaceView is null.");
            return;
        }
        if (info instanceof CatanGameState) {

            CatanGameState next = new CatanGameState((CatanGameState) info);
            CatanGameState previousState = state;
            int previousTurn = state == null ? -1 : state.getCurrentPlayerId();
            CatanMarketState.Event previousEvent = state == null ? null
                    : state.getMarketState().getActiveEvent();
            CatanMarketState.Event arrivingEvent = next.getMarketState().getActiveEvent();
            boolean newMarketEvent = arrivingEvent != null
                    && arrivingEvent != previousEvent;
            String buildConfirmation = confirmPendingBuild(next);
            int cardDrawn = findNewDevelopmentCard(next);
            this.state = next;
            // Clear stale selections on accepted state updates, not on a
            // rejected build; rejection should preserve the attempted point.
            if (pendingBuild == PENDING_NONE) {
                selectedIntersections.clear();
                if (boardSurfaceView.getGrid() != null)
                    boardSurfaceView.getGrid().clearHighLightedIntersections();
            }
            captureOwnResourceChanges();
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
            if (myActivity != null) {
                if (newMarketEvent)
                    CatanExperienceFeedback.notice(myActivity,
                            next.getMarketState().announcement(), true);
                else if (previousEvent != null && arrivingEvent == null)
                    CatanExperienceFeedback.notice(myActivity, "Market event ended", false);
                if (previousTurn >= 0 && previousTurn != playerNum
                        && next.getCurrentPlayerId() == playerNum && !next.isSetupPhase())
                    CatanExperienceFeedback.notice(myActivity, "Your turn", true);
            }
            fitPopupOnScreen(robberDiscardGroup);
            fitPopupOnScreen(robberChooseHexGroup);
            fitPopupOnScreen(developmentGroup);
            fitPopupOnScreen(pickResourceGroup);
            drawGraphics();
            showBoardProgress(previousState, next);
            if (buildConfirmation != null) {
                messageTextView.setText(buildConfirmation);
                Toast.makeText(myActivity, buildConfirmation, Toast.LENGTH_SHORT).show();
            }
            if (cardDrawn >= 0) {
                messageTextView.setText("Development card drawn — added to your hand.");
                CatanDevelopmentCardsUI.showDrawnCard(myActivity, cardDrawn);
            }

        } else if (info instanceof NotYourTurnInfo) {
            if (developmentPurchasePending) rejectDevelopmentPurchase(true);
            else rejectPendingBuild(true);
            Log.i(TAG, "receiveInfo: Player tried to make action but it is not their turn.");
        } else if (info instanceof IllegalMoveInfo) {
            if (developmentPurchasePending) rejectDevelopmentPurchase(false);
            else rejectPendingBuild(false);
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
        CatanExperienceSettings.load(activity);
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
        generalMusic.setVolume(0, 0);
        // Never make an unsolicited sound on initial launch.
        if (CatanExperienceSettings.music(myActivity)) {
            generalMusic.setVolume(1, 1);
            generalMusic.start();
        }
        newGame = false;
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

        Button hudToggle = activity.findViewById(R.id.hud_toggle);
        if (hudToggle != null) hudToggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                hudExpanded = !hudExpanded;
                applyHudVisibility();
            }
        });
        Button resetCamera = activity.findViewById(R.id.board_reset_view);
        if (resetCamera != null) resetCamera.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (boardSurfaceView != null) boardSurfaceView.resetCamera();
            }
        });
        applyHudVisibility();
        hudLastRoll = activity.findViewById(R.id.hud_last_roll);
        hudHandFirst = activity.findViewById(R.id.hud_hand_first);
        hudHandSecond = activity.findViewById(R.id.hud_hand_second);
        hudCards = activity.findViewById(R.id.hud_development_cards);
        if (hudCards != null) {
            hudCards.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { showDevelopmentCollection(); }
            });
        }
        hudLatestAction = activity.findViewById(R.id.hud_latest_action);
        Button historyButton = activity.findViewById(R.id.hud_history_button);
        if (historyButton != null) historyButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override public void onClick(View v) { showGameplayHistory(); }
                });
        Button statistics = activity.findViewById(R.id.hud_stats_button);
        if (statistics != null) statistics.setOnClickListener(view -> {
            if (state != null) CatanCareerUi.showCurrent(
                    myActivity, new CatanGameState(state), getAllPlayerNames());
        });
        Button experience = activity.findViewById(R.id.hud_experience_button);
        if (experience != null) experience.setOnClickListener(view ->
                CatanExperienceSettings.show(myActivity, this::updateExperienceAudio));
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
                return "Use Trade → Players to exchange cards with computer opponents.\n"
                        + "You can compare all responses before selecting one deal.\n\n"
                        + "Use Trade → Bank & Harbours for automatic exchange rates:\n"
                        + "4:1 at the bank, 3:1 at general harbours and 2:1 at specialist harbours.\n"
                        + "The game detects harbours you own; no board selection is required.\n\n"
                        + "Optional surprise Market Events may temporarily change trading rates.";
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
     * Build 011: a single, native landscape-safe Trade panel with TWO tabs.
     * Reuses the existing sidebar button and scrolls on smaller phones.
     */
    private void showTradeDialog() {
        if (myActivity == null || state == null) return;
        final String[] resources = {"Brick", "Grain", "Lumber", "Ore", "Wool"};
        final int pad = (int)(10 * myActivity.getResources().getDisplayMetrics().density);
        final int[] offer = new int[5], want = new int[5];
        LinearLayout outer = new LinearLayout(myActivity);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(pad, pad / 2, pad, pad / 2);

        LinearLayout tabs = new LinearLayout(myActivity);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        Button playersTab = new Button(myActivity), bankTab = new Button(myActivity);
        playersTab.setText("Players");
        bankTab.setText("Bank & Harbours");
        tabs.addView(playersTab, new LinearLayout.LayoutParams(0, -2, 1));
        tabs.addView(bankTab, new LinearLayout.LayoutParams(0, -2, 1));
        outer.addView(tabs);

        ScrollView scroll = new ScrollView(myActivity);
        scroll.setFillViewport(false);
        LinearLayout panels = new LinearLayout(myActivity);
        panels.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(panels);
        // A wrap-content AlertDialog may measure a 0dp weighted child at
        // zero height in landscape. Give the content a real, bounded height,
        // and scroll *inside* it instead of covering the board or buttons.
        int screenHeight = myActivity.getResources().getDisplayMetrics().heightPixels;
        int contentHeight = Math.max(pad * 12,
                (int) (screenHeight * 0.50f));
        outer.addView(scroll, new LinearLayout.LayoutParams(-1, contentHeight));

        // Players tab: five compact rows, one per resource, with offer/want
        // quantities side by side. No ten-screen resource pickers or overlays.
        LinearLayout playersPanel = new LinearLayout(myActivity);
        playersPanel.setOrientation(LinearLayout.VERTICAL);
        panels.addView(playersPanel);
        final TextView[] offerDisplays = new TextView[5];
        final TextView[] wantDisplays = new TextView[5];
        TextView headings = new TextView(myActivity);
        headings.setText("RESOURCE                 YOU GIVE                     YOU WANT");
        headings.setTextSize(11f);
        headings.setPadding(0, pad, 0, pad / 2);
        playersPanel.addView(headings);
        for (int resource = 0; resource < 5; resource++) {
            final int r = resource;
            LinearLayout line = new LinearLayout(myActivity);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = new TextView(myActivity);
            title.setText(resources[r] + " (" + state.getPlayerList()
                    .get(playerNum).getResourceCards()[r] + ")");
            title.setTextSize(12f);
            line.addView(title, new LinearLayout.LayoutParams(0, -2, 1.5f));
            offerDisplays[r] = addTradeCounter(line, offer, r, true, pad);
            wantDisplays[r] = addTradeCounter(line, want, r, false, pad);
            playersPanel.addView(line);
        }
        TextView help = new TextView(myActivity);
        help.setText("Both sides must offer resources. Send to one computer or everyone.");
        help.setTextSize(12f);
        help.setPadding(0, pad, 0, pad / 2);
        playersPanel.addView(help);
        Button suggest = new Button(myActivity);
        suggest.setText("Suggest a useful trade");
        suggest.setAllCaps(false);
        suggest.setOnClickListener(v -> {
            int[][] suggestion = CatanNegotiationAI.proposeOnOwnTurn(state, playerNum);
            if (suggestion == null) {
                Toast.makeText(myActivity,
                        "No useful trade suggestion from your current cards.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            for (int r = 0; r < 5; r++) {
                offer[r] = suggestion[0][r];
                want[r] = suggestion[1][r];
                offerDisplays[r].setText(String.valueOf(offer[r]));
                wantDisplays[r].setText(String.valueOf(want[r]));
            }
            help.setText("Suggested from your next possible build. "
                    + "You can adjust it before sending; acceptance is not guaranteed.");
        });
        playersPanel.addView(suggest);
        if (previousPlayerTradeGive != null && previousPlayerTradeWant != null) {
            Button repeat = new Button(myActivity);
            repeat.setText("Repeat last offer");
            repeat.setAllCaps(false);
            repeat.setOnClickListener(v -> {
                for (int r = 0; r < 5; r++) {
                    offer[r] = previousPlayerTradeGive[r];
                    want[r] = previousPlayerTradeWant[r];
                    offerDisplays[r].setText(String.valueOf(offer[r]));
                    wantDisplays[r].setText(String.valueOf(want[r]));
                }
                help.setText("Previous offer loaded. Check your current hand before sending.");
            });
            playersPanel.addView(repeat);
        }

        ArrayList<String> targets = new ArrayList<>();
        ArrayList<Integer> targetIds = new ArrayList<>();
        targets.add("All computer opponents");
        targetIds.add(-1);
        for (int i = 0; i < state.getPlayerList().size(); i++) {
            if (i == playerNum) continue;
            targets.add("Computer " + i);
            targetIds.add(i);
        }
        Spinner target = new Spinner(myActivity);
        target.setAdapter(new ArrayAdapter<String>(myActivity,
                android.R.layout.simple_spinner_dropdown_item, targets));
        playersPanel.addView(target);

        // Bank tab: the game recognises all owned harbours automatically.
        LinearLayout bankPanel = new LinearLayout(myActivity);
        bankPanel.setOrientation(LinearLayout.VERTICAL);
        panels.addView(bankPanel);
        TextView explanation = new TextView(myActivity);
        explanation.setText("Best available bank or harbour exchange rate is automatic.");
        explanation.setTextSize(13f);
        bankPanel.addView(explanation);
        if (state.getMarketState().getActiveEvent() != null) {
            TextView marketNotice = new TextView(myActivity);
            marketNotice.setText(state.getMarketState().announcement());
            marketNotice.setTextSize(12f);
            marketNotice.setPadding(0, pad / 2, 0, pad / 2);
            bankPanel.addView(marketNotice);
        }

        TextView givingLabel = new TextView(myActivity);
        givingLabel.setText("You give");
        givingLabel.setPadding(0, pad, 0, 0);
        bankPanel.addView(givingLabel);
        Spinner give = new Spinner(myActivity);
        give.setAdapter(new ArrayAdapter<String>(myActivity,
                android.R.layout.simple_spinner_dropdown_item, resources));
        bankPanel.addView(give);
        TextView receiveLabel = new TextView(myActivity);
        receiveLabel.setText("You receive");
        receiveLabel.setPadding(0, pad, 0, 0);
        bankPanel.addView(receiveLabel);
        Spinner receive = new Spinner(myActivity);
        receive.setAdapter(new ArrayAdapter<String>(myActivity,
                android.R.layout.simple_spinner_dropdown_item, resources));
        receive.setSelection(1);
        bankPanel.addView(receive);
        TextView preview = new TextView(myActivity);
        preview.setTextSize(13f);
        preview.setPadding(0, pad, 0, pad);
        bankPanel.addView(preview);
        Runnable refresh = () -> {
            int giving = give.getSelectedItemPosition();
            int receiving = receive.getSelectedItemPosition();
            if (giving < 0 || receiving < 0) return;
            int rate = CatanTradingRules.bestRate(state, playerNum, giving, receiving);
            int held = state.getPlayerList().get(playerNum).getResourceCards()[giving];
            if (giving == receiving) preview.setText("Choose different resources.");
            else if (rate < 1) preview.setText("This exchange is currently unavailable.");
            else preview.setText("Give " + rate + " " + resources[giving]
                    + " → Receive 1 " + resources[receiving]
                    + "   (you hold " + held + ")"
                    + (held < rate ? "\nNot enough cards yet." : ""));
        };
        android.widget.AdapterView.OnItemSelectedListener listener =
                new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override public void onItemSelected(AdapterView<?> parent,
                    View view, int position, long id) { refresh.run(); }
        };
        give.setOnItemSelectedListener(listener);
        receive.setOnItemSelectedListener(listener);
        refresh.run();

        AlertDialog dialog = new AlertDialog.Builder(myActivity)
                .setTitle("Trade")
                .setView(outer)
                .setPositiveButton("Send Offer", null)
                .setNegativeButton("Close", null)
                .create();
        final boolean[] showingPlayers = {true};
        Runnable selectTab = () -> {
            playersPanel.setVisibility(showingPlayers[0] ? View.VISIBLE : View.GONE);
            bankPanel.setVisibility(showingPlayers[0] ? View.GONE : View.VISIBLE);
            playersTab.setEnabled(!showingPlayers[0]);
            bankTab.setEnabled(showingPlayers[0]);
            if (dialog.isShowing()) dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setText(showingPlayers[0] ? "Send Offer" : "Trade");
        };
        playersTab.setOnClickListener(view -> { showingPlayers[0] = true; selectTab.run(); });
        bankTab.setOnClickListener(view -> { showingPlayers[0] = false; selectTab.run(); });
        selectTab.run();
        dialog.setOnShowListener(ignored -> {
            dialog.getWindow().setLayout(
                    (int)(myActivity.getResources().getDisplayMetrics().widthPixels * .88f),
                    (int)(myActivity.getResources().getDisplayMetrics().heightPixels * .88f));
            selectTab.run();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                boolean turn = state != null && state.getCurrentPlayerId() == playerNum
                        && state.isActionPhase() && !state.isSetupPhase()
                        && !state.isRobberPhase();
                if (!turn) {
                    Toast.makeText(myActivity, "Trade after rolling on your turn.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (showingPlayers[0]) {
                    if (!CatanTradingRules.validBundle(offer)
                            || !CatanTradingRules.validBundle(want)) {
                        Toast.makeText(myActivity, "Both sides must offer resources.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    for (int r = 0; r < 5; r++) {
                        if (offer[r] > state.getPlayerList().get(playerNum)
                                .getResourceCards()[r] || (offer[r] > 0 && want[r] > 0)) {
                            Toast.makeText(myActivity,
                                    "Check your cards. Do not request the same resource you give.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    int selectedTarget = targetIds.get(target.getSelectedItemPosition());
                    int[] invited;
                    if (selectedTarget >= 0) invited = new int[]{selectedTarget};
                    else {
                        invited = new int[state.getPlayerList().size() - 1];
                        int count = 0;
                        for (int id = 0; id < state.getPlayerList().size(); id++)
                            if (id != playerNum) invited[count++] = id;
                    }
                    previousPlayerTradeGive = offer.clone();
                    previousPlayerTradeWant = want.clone();
                    game.sendAction(new CatanTradeOfferAction(this, invited, offer, want));
                } else {
                    int giving = give.getSelectedItemPosition();
                    int receiving = receive.getSelectedItemPosition();
                    int rate = CatanTradingRules.bestRate(state, playerNum, giving, receiving);
                    if (rate < 1 || !state.getPlayerList().get(playerNum)
                            .checkResourceCard(giving, rate)) {
                        Toast.makeText(myActivity, "Trade unavailable. Check your resources.",
                                Toast.LENGTH_LONG).show();
                        refresh.run();
                        return;
                    }
                    game.sendAction(new CatanTradeWithAutoBankAction(this, giving, receiving));
                }
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private TextView addTradeCounter(LinearLayout row, int[] quantities, int resource,
                                 boolean spending, int pad) {
        LinearLayout control = new LinearLayout(myActivity);
        control.setOrientation(LinearLayout.HORIZONTAL);
        control.setGravity(Gravity.CENTER_VERTICAL);
        Button minus = new Button(myActivity), plus = new Button(myActivity);
        TextView count = new TextView(myActivity);
        minus.setText("−");
        plus.setText("+");
        minus.setMinWidth(0);
        plus.setMinWidth(0);
        minus.setMinimumWidth(0);
        plus.setMinimumWidth(0);
        minus.setPadding(0, 0, 0, 0);
        plus.setPadding(0, 0, 0, 0);
        count.setText("0");
        count.setGravity(Gravity.CENTER);
        count.setMinWidth(pad * 2);
        control.addView(minus, new LinearLayout.LayoutParams(pad * 3, pad * 4));
        control.addView(count, new LinearLayout.LayoutParams(pad * 2, pad * 4));
        control.addView(plus, new LinearLayout.LayoutParams(pad * 3, pad * 4));
        minus.setOnClickListener(v -> {
            quantities[resource] = Math.max(0, quantities[resource] - 1);
            count.setText(String.valueOf(quantities[resource]));
        });
        plus.setOnClickListener(v -> {
            int allowed = spending ? state.getPlayerList().get(playerNum)
                    .getResourceCards()[resource] : 20;
            quantities[resource] = Math.min(allowed, quantities[resource] + 1);
            count.setText(String.valueOf(quantities[resource]));
        });
        row.addView(control, new LinearLayout.LayoutParams(0, -2, 2f));
        return count;
    }

    private static String tradeBundleDescription(int[] cards) {
        String[] names = {"Brick", "Grain", "Lumber", "Ore", "Wool"};
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (cards[i] <= 0) continue;
            if (out.length() > 0) out.append(" + ");
            out.append(cards[i]).append(" ").append(names[i]);
        }
        return out.toString();
    }

    /** A CPU may initiate a trade on its OWN turn; the human can respond. */
    private void showIncomingTradeOffer(CatanTradeInfo request) {
        if (myActivity == null || state == null || request.getProposerId() < 0
                || request.getProposerId() >= state.getPlayerList().size()) return;
        String other = allPlayerNames[request.getProposerId()];
        String deal = other + " offers " + tradeBundleDescription(request.getGiving())
                + "\nYou would give " + tradeBundleDescription(request.getReceiving());
        LinearLayout content = new LinearLayout(myActivity);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(14 * myActivity.getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding / 2, padding, padding / 2);
        TextView dealText = new TextView(myActivity);
        dealText.setText(deal);
        dealText.setTextSize(14f);
        content.addView(dealText);
        android.widget.CheckBox mute = new android.widget.CheckBox(myActivity);
        mute.setText("Mute computer trade offers until next round");
        mute.setTextSize(12f);
        content.addView(mute);
        AlertDialog prompt = new AlertDialog.Builder(myActivity)
                .setTitle(other + " wants to trade")
                .setView(content)
                .setPositiveButton("Accept", null)
                .setNeutralButton("Counter", (dialog, option) ->
                        showHumanTradeCounter(request, mute.isChecked()))
                .setNegativeButton("Decline", (dialog, option) ->
                        game.sendAction(new CatanTradeReplyAction(
                                this, request.getOfferId(),
                                CatanTradeReplyAction.DECLINE, null, null,
                                mute.isChecked())))
                .setCancelable(false)
                .create();
        prompt.setOnShowListener(ignored ->
                prompt.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (!state.getPlayerList().get(playerNum)
                            .hasResourceBundle(request.getReceiving())) {
                        Toast.makeText(myActivity, "You do not have the cards requested.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    game.sendAction(new CatanTradeReplyAction(this, request.getOfferId(),
                            CatanTradeReplyAction.ACCEPT, null, null,
                            mute.isChecked()));
                    prompt.dismiss();
                }));
        prompt.show();
    }

    /** User-controlled audio remains silent by default and changes live. */
    private void updateExperienceAudio() {
        if (myActivity == null || generalMusic == null) return;
        try {
            boolean enabled = CatanExperienceSettings.music(myActivity);
            generalMusic.setVolume(enabled ? 1f : 0f, enabled ? 1f : 0f);
            if (enabled && !generalMusic.isPlaying()) generalMusic.start();
            if (!enabled && generalMusic.isPlaying()) generalMusic.pause();
            if (!enabled && menuMusic != null && menuMusic.isPlaying()) menuMusic.pause();
        } catch (IllegalStateException ignored) {
            // The current activity may be reinitialising its old media player.
        }
    }

    /** One editable counteroffer; quantities are from the COMPUTER's perspective. */
    private void showHumanTradeCounter(CatanTradeInfo request, boolean muteThisRound) {
        LinearLayout content = new LinearLayout(myActivity);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (10 * myActivity.getResources().getDisplayMetrics().density);
        TextView hint = new TextView(myActivity);
        hint.setText("RESOURCE                COMPUTER GIVES                YOU GIVE");
        hint.setTextSize(11f);
        content.addView(hint);
        final int[] cpuGives = request.getGiving();
        final int[] humanGives = request.getReceiving();
        String[] resources = {"Brick", "Grain", "Lumber", "Ore", "Wool"};
        for (int i = 0; i < 5; i++) {
            LinearLayout row = new LinearLayout(myActivity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = new TextView(myActivity);
            name.setText(resources[i]);
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1.5f));
            addTradeCounterWithCount(row, cpuGives, i, false, pad);
            addTradeCounterWithCount(row, humanGives, i, true, pad);
            content.addView(row);
        }
        ScrollView scroll = new ScrollView(myActivity);
        scroll.addView(content);
        AlertDialog counter = new AlertDialog.Builder(myActivity)
                .setTitle("Counteroffer")
                .setView(scroll)
                .setPositiveButton("Send counter", null)
                .setNegativeButton("Decline", (dialog, option) ->
                        game.sendAction(new CatanTradeReplyAction(this, request.getOfferId(),
                                CatanTradeReplyAction.DECLINE, null, null,
                                muteThisRound)))
                .setCancelable(false)
                .create();
        counter.setOnShowListener(ignored -> {
            // The counteroffer has five rows of controls. Bound the dialog
            // to the landscape window, scrolling inside rather than cutting
            // off Cancel/Send or obscuring the game permanently.
            if (counter.getWindow() != null) {
                counter.getWindow().setLayout(
                        (int) (myActivity.getResources().getDisplayMetrics().widthPixels * .88f),
                        (int) (myActivity.getResources().getDisplayMetrics().heightPixels * .86f));
            }
            counter.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (!CatanTradingRules.validBundle(cpuGives)
                        || !CatanTradingRules.validBundle(humanGives)
                        || CatanTradingRules.overlaps(cpuGives, humanGives)
                        || !state.getPlayerList().get(playerNum)
                                .hasResourceBundle(humanGives)) {
                    Toast.makeText(myActivity,
                            "Choose cards you can afford; do not give and receive the same type.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                game.sendAction(new CatanTradeReplyAction(this, request.getOfferId(),
                        CatanTradeReplyAction.COUNTER, cpuGives, humanGives,
                        muteThisRound));
                counter.dismiss();
            });
        });
        counter.show();
    }

    /** Use the existing small resource counter, but honour starting quantities. */
    private void addTradeCounterWithCount(LinearLayout row, int[] quantities,
                                          int resource, boolean spending, int pad) {
        LinearLayout control = new LinearLayout(myActivity);
        control.setOrientation(LinearLayout.HORIZONTAL);
        control.setGravity(Gravity.CENTER_VERTICAL);
        Button minus = new Button(myActivity), plus = new Button(myActivity);
        TextView amount = new TextView(myActivity);
        minus.setText("−");
        plus.setText("+");
        minus.setPadding(0, 0, 0, 0);
        plus.setPadding(0, 0, 0, 0);
        amount.setText(String.valueOf(quantities[resource]));
        amount.setGravity(Gravity.CENTER);
        control.addView(minus, new LinearLayout.LayoutParams(pad * 3, pad * 4));
        control.addView(amount, new LinearLayout.LayoutParams(pad * 2, pad * 4));
        control.addView(plus, new LinearLayout.LayoutParams(pad * 3, pad * 4));
        minus.setOnClickListener(v -> {
            quantities[resource] = Math.max(0, quantities[resource] - 1);
            amount.setText(String.valueOf(quantities[resource]));
        });
        plus.setOnClickListener(v -> {
            int limit = spending ? state.getPlayerList().get(playerNum)
                    .getResourceCards()[resource] : 20;
            quantities[resource] = Math.min(limit, quantities[resource] + 1);
            amount.setText(String.valueOf(quantities[resource]));
        });
        row.addView(control, new LinearLayout.LayoutParams(0, -2, 2f));
    }

    /** View every CPU response before selecting one. Declining costs nothing. */
    private void showTradeComparisons(CatanTradeResponsesInfo results) {
        if (myActivity == null || state == null) return;
        List<CatanTradeResponsesInfo.Response> responses = results.getResponses();
        if (responses.isEmpty()) {
            Toast.makeText(myActivity, "No computer opponents available.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String[] choices = new String[responses.size()];
        for (int i = 0; i < responses.size(); i++) {
            CatanTradeResponsesInfo.Response response = responses.get(i);
            String verdict = response.getVerdict() == CatanTradeResponsesInfo.ACCEPT
                    ? "ACCEPTS" : response.getVerdict() == CatanTradeResponsesInfo.COUNTER
                        ? "COUNTEROFFERS" : "DECLINES";
            choices[i] = "Computer " + response.getPlayerId() + " · " + verdict
                    + "\nYou give: " + tradeBundleDescription(response.getGive())
                    + "\nYou receive: " + tradeBundleDescription(response.getWant());
        }
        new AlertDialog.Builder(myActivity)
                .setTitle("Compare all offers")
                .setCancelable(false)
                .setItems(choices, (dialog, which) -> {
                    CatanTradeResponsesInfo.Response picked = responses.get(which);
                    if (picked.getVerdict() == CatanTradeResponsesInfo.DECLINE) {
                        showTradeComparisons(results);
                        return;
                    }
                    int[] giving = picked.getGive(), receiving = picked.getWant();
                    if (!state.getPlayerList().get(playerNum).hasResourceBundle(giving)) {
                        Toast.makeText(myActivity,
                                "Not enough cards for that counteroffer.",
                                Toast.LENGTH_LONG).show();
                        showTradeComparisons(results);
                        return;
                    }
                    new AlertDialog.Builder(myActivity)
                            // Android Back/outside tap must not silently strand
                            // a pending trade after the comparison list closes.
                            .setCancelable(false)
                            .setTitle("Confirm trade with Computer " + picked.getPlayerId())
                            .setMessage("You give: " + tradeBundleDescription(giving)
                                    + "\nYou receive: " + tradeBundleDescription(receiving))
                            .setPositiveButton("Complete trade", (confirm, button) ->
                                    game.sendAction(new CatanTradeChooseAction(
                                            this, results.getOfferId(), picked.getPlayerId())))
                            .setNegativeButton("Back", (confirm, button) ->
                                    showTradeComparisons(results))
                            .show();
                })
                .setNegativeButton("Cancel offer", (dialog, which) ->
                        game.sendAction(new CatanTradeChooseAction(
                                this, results.getOfferId(), -1)))
                .show();
    }

    /**
     * The original controls remain functional. Only their visibility and
     * allocated screen width change when the player collapses the control rail.
     */
    /** The island must remain accessible without a large permanent overlay. */
    private void applyHudVisibility() {
        if (myActivity == null) return;
        View panel = myActivity.findViewById(R.id.gameplay_hud);
        if (panel != null) panel.setVisibility(hudExpanded ? View.VISIBLE : View.GONE);
        Button toggle = myActivity.findViewById(R.id.hud_toggle);
        if (toggle != null)
            toggle.setText(hudExpanded ? "Hide dice & hand ▴" : "Dice & hand ▾");
    }

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
        if (playerNum < armyTrophyImages.length)
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
        if (playerNum < roadTrophyImages.length)
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
        // Engine archives the result immediately after announcing victory.
        // Delay the visual summary briefly so the atomic career write finishes.
        if (myActivity != null && state != null) {
            CatanExperienceSounds.play(myActivity, CatanExperienceSounds.Cue.VICTORY);
            final String matchId = state.getMatchStats().getMatchId();
            myActivity.runOnUiThread(() ->
                new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(() -> {
                        if (myActivity != null && !myActivity.isFinishing())
                            CatanCareerUi.showLatest(myActivity, matchId);
                    }, 650));
        } else {
            super.gameIsOver(message);
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

