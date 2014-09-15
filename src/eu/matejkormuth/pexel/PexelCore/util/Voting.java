package eu.matejkormuth.pexel.PexelCore.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import eu.matejkormuth.pexel.PexelCore.Pexel;

/**
 * Class used for voting.
 */
public abstract class Voting {
    private List<Player>            voters;
    private final Map<Player, Vote> votes           = new HashMap<Player, Vote>();
    private final String            voteSubject;
    private long                    lastInteraction = Long.MAX_VALUE;
    private long                    timeout         = 20 * 5;
    private boolean                 canVoteOnlyOnce = true;
    private int                     taskId          = 0;
    
    public Voting(final String voteSubject) {
        this.voteSubject = voteSubject;
    }
    
    /**
     * Starts this vote.
     * 
     * @param voters
     *            players that should be able to vote
     * @param invoker
     *            player that invoked the vote
     */
    public void invoke(final List<Player> voters, final Player invoker) {
        this.voters = voters;
        this.taskId = Pexel.getScheduler().scheduleSyncRepeatingTask(new Runnable() {
            @Override
            public void run() {
                Voting.this.timeout();
            }
        }, 0L, 20L);
        this.startVote(invoker);
        this.lastInteraction = System.currentTimeMillis();
    }
    
    /**
     * Called when voting should timeout.
     */
    protected void timeout() {
        if (this.lastInteraction + this.timeout < System.currentTimeMillis()) {
            Pexel.cancelTask(this.taskId);
            this.onVoteFailed();
        }
    }
    
    /**
     * Sends message to all voters.
     */
    private void startVote(final Player invoker) {
        this.broadcast("Player " + invoker.getName() + " started the vote for "
                + this.voteSubject + ".");
    }
    
    public void broadcast(final String message) {
        for (Player p : this.voters)
            p.sendMessage(ChatColor.GOLD + "[VOTE] " + message);
    }
    
    /**
     * Votes positively for specified player.
     * 
     * @param voter
     *            player that votes for subject
     */
    public void vote(final Player voter, final Vote vote) {
        if (!this.voters.contains(voter))
            throw new RuntimeException("Invalid voter! Specified voter can't vote.");
        if (this.votes.containsKey(voter) && this.canVoteOnlyOnce)
            throw new RuntimeException("Invalid vote! One player can vote only once!");
        
        this.votes.put(voter, vote);
        this.broadcastState();
        this.processEnd();
        
        this.lastInteraction = System.currentTimeMillis();
    }
    
    /**
     * Check for voting end.
     */
    private void processEnd() {
        int yesVotes = 0;
        for (Vote value : this.votes.values())
            if (value == Vote.YES)
                yesVotes++;
        
        if (yesVotes == this.voters.size())
            this.onVoteSucceeded();
        else if (yesVotes > (this.voters.size() / 2))
            this.onVoteSucceeded();
        else
            this.onVoteFailed();
    }
    
    /**
     * Broadcast vote state to all players.
     */
    private void broadcastState() {
        int yesVotes = 0;
        for (Vote value : this.votes.values())
            if (value == Vote.YES)
                yesVotes++;
        
        this.broadcast(yesVotes + "/" + this.voters.size() + " players voted for "
                + this.voteSubject + "!");
    }
    
    // Abstract functions
    
    /**
     * Called when vote failed.
     */
    public abstract void onVoteFailed();
    
    /**
     * Called when vote succeeded.
     */
    public abstract void onVoteSucceeded();
    
    // Getters and setters
    
    public long getTimeout() {
        return this.timeout;
    }
    
    public void setTimeout(final long timeout) {
        this.timeout = timeout;
    }
    
    public boolean canVoteOnlyOnce() {
        return this.canVoteOnlyOnce;
    }
    
    public void setCanVoteOnlyOnce(final boolean canVoteOnlyOnce) {
        this.canVoteOnlyOnce = canVoteOnlyOnce;
    }
}