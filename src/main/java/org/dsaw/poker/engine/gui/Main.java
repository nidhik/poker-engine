// This file is part of the 'texasholdem' project, an open source
// Texas Hold'em poker application written in Java.
//
// Copyright 2009 Oscar Stigter
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.dsaw.poker.engine.gui;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

import org.dsaw.poker.engine.Card;
import org.dsaw.poker.engine.Client;
import org.dsaw.poker.engine.Player;
import org.dsaw.poker.engine.Table;
import org.dsaw.poker.engine.TableType;
import org.dsaw.poker.engine.actions.Action;
import org.dsaw.poker.engine.bots.BasicBot;

/**
 * The game's main frame.
 * 
 * This is the core class of the Swing UI client application.
 * 
 * @author Oscar Stigter
 */
public class Main extends JFrame implements Client, Runnable{
    
    /** Serial version UID. */
    private static final long serialVersionUID = -5414633931666096443L;
    
    /** Table type (betting structure). */
    private static final TableType TABLE_TYPE = TableType.NO_LIMIT;

    /** The size of the big blind. */
    private static final BigDecimal BIG_BLIND = BigDecimal.valueOf(10);

    /** The starting cash per player. */
    private static final BigDecimal STARTING_CASH = BigDecimal.valueOf(500);

    /** The GridBagConstraints. */
    private final GridBagConstraints gc;
    
    /** The board panel. */
    private final BoardPanel boardPanel;
    
    /** The control panel. */
    private final ControlPanel controlPanel;
    
    /** The player panels. */
    private final Map<String, PlayerPanel> playerPanels;
    
    /** The player. */
    private final Player humanPlayer;
    
    /** The current dealer's name. */
    private String dealerName; 

    /** The current actor's name. */
    private String actorName;

    /**
     * Constructor.
     */
    public Main(String me, TableType tableType) {
        super("Texas Hold'em poker");
        humanPlayer = new Player(me, STARTING_CASH, this);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setBackground(UIConstants.TABLE_COLOR);
        setLayout(new GridBagLayout());

        gc = new GridBagConstraints();
        
        controlPanel = new ControlPanel(tableType);
        
        boardPanel = new BoardPanel(controlPanel);        
        addComponent(boardPanel, 1, 1, 1, 1);


        playerPanels = new HashMap<>();

    }

    public void showFrame() {
        // Show the frame.
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public Player getHumanPlayer() {
        return humanPlayer;
    }

    public void setPlayers(Map<String, Player> players) {

        int i = 0;
        for (Player player : players.values()) {
            PlayerPanel panel = new PlayerPanel();
            playerPanels.put(player.getName(), panel);
            switch (i++) {
                case 0:
                    // North position.
                    addComponent(panel, 1, 0, 1, 1);
                    break;
                case 1:
                    // East position.
                    addComponent(panel, 2, 1, 1, 1);
                    break;
                case 2:
                    // South position.
                    addComponent(panel, 1, 2, 1, 1);
                    break;
                case 3:
                    // West position.
                    addComponent(panel, 0, 1, 1, 1);
                    break;
                default:
                    // Do nothing.
            }
        }
    }
    /**
     * The application's entry point.
     *
     * @param args
     *            The command line arguments.
     */
    public static void main(String[] args) {



   /* The players at the table. */
        Map<String, Player> players = new LinkedHashMap<>();

        Main gui1 = new Main("Nidhi", TABLE_TYPE);
        Main gui2 = new Main("Ada", TABLE_TYPE);
        Main gui3 = new Main("Mo", TABLE_TYPE);
        Main gui4 = new Main("Neha", TABLE_TYPE);

        final List<Main> gs = Arrays.asList(gui1, gui2, gui3, gui4);

       Player p1 = gui1.getHumanPlayer();
       Player p2 = gui2.getHumanPlayer();
       Player p3 = gui3.getHumanPlayer();
       Player p4 = gui4.getHumanPlayer();

       players.put(p1.getName(), p1);
       players.put(p2.getName(), p2);
       players.put(p3.getName(), p3);
       players.put(p4.getName(), p4);

        gui1.setPlayers(players);
        gui2.setPlayers(players);
        gui3.setPlayers(players);
        gui4.setPlayers(players);


          /* The table. */
        final Table table = new Table(TABLE_TYPE, BIG_BLIND);
        for (Player player : players.values()) {
            table.addPlayer(player);
        }

        int numberOfSimultaneousExecutions = 5;
        java.util.concurrent.Executor executor = java.util.concurrent.Executors.newFixedThreadPool(numberOfSimultaneousExecutions);

        for (int i = 0; i < numberOfSimultaneousExecutions - 1; i++) {

            final Main g = gs.get(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    g.showFrame();
                }
            });
        }


        executor.execute(new Runnable() {
            @Override
            public void run() {
                // Start the game.
                table.run();
            }
        });


    }

    @Override
    public void joinedTable(TableType type, BigDecimal bigBlind, List<Player> players) {
        for (Player player : players) {
            PlayerPanel playerPanel = playerPanels.get(player.getName());
            if (playerPanel != null) {
                playerPanel.update(player);
            }
        }
    }

    @Override
    public void messageReceived(String message) {
        boardPanel.setMessage(message);
        boardPanel.waitForUserInput();
    }

    @Override
    public void handStarted(Player dealer) {
        setDealer(false);
        dealerName = dealer.getName();
        setDealer(true);
    }

    @Override
    public void actorRotated(Player actor) {
        setActorInTurn(false);
        actorName = actor.getName();
        setActorInTurn(true);
    }

    @Override
    public void boardUpdated(List<Card> cards, BigDecimal bet, BigDecimal pot) {
        boardPanel.update(cards, bet, pot);
    }

    @Override
    public void playerUpdated(Player player) {
        PlayerPanel playerPanel = playerPanels.get(player.getName());
        if (playerPanel != null) {
            playerPanel.update(player);
        }
    }

    @Override
    public void playerActed(Player player) {
        String name = player.getName();
        PlayerPanel playerPanel = playerPanels.get(name);
        if (playerPanel != null) {
            playerPanel.update(player);
            Action action = player.getAction();
            if (action != null) {
                boardPanel.setMessage(String.format("%s %s.", name, action.getVerb()));
                if (player.getClient() != this) {
                    boardPanel.waitForUserInput();
                }
            }
        } else {
            throw new IllegalStateException(
                    String.format("No PlayerPanel found for player '%s'", name));
        }
    }

    @Override
    public Action act(BigDecimal minBet, BigDecimal currentBet, Set<Action> allowedActions) {
        boardPanel.setMessage("Please select an action:");
        return controlPanel.getUserInput(minBet, humanPlayer.getCash(), allowedActions);
    }

    /**
     * Adds an UI component.
     * 
     * @param component
     *            The component.
     * @param x
     *            The column.
     * @param y
     *            The row.
     * @param width
     *            The number of columns to span.
     * @param height
     *            The number of rows to span.
     */
    private void addComponent(Component component, int x, int y, int width, int height) {
        gc.gridx = x;
        gc.gridy = y;
        gc.gridwidth = width;
        gc.gridheight = height;
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        getContentPane().add(component, gc);
    }

    /**
     * Sets whether the actor  is in turn.
     * 
     * @param isInTurn
     *            Whether the actor is in turn.
     */
    private void setActorInTurn(boolean isInTurn) {
        if (actorName != null) {
            PlayerPanel playerPanel = playerPanels.get(actorName);
            if (playerPanel != null) {
                playerPanel.setInTurn(isInTurn);
            }
        }
    }

    /**
     * Sets the dealer.
     * 
     * @param isDealer
     *            Whether the player is the dealer.
     */
    private void setDealer(boolean isDealer) {
        if (dealerName != null) {
            PlayerPanel playerPanel = playerPanels.get(dealerName);
            if (playerPanel != null) {
                playerPanel.setDealer(isDealer);
            }
        }
    }

    @Override
    public void run() {

    }
}
