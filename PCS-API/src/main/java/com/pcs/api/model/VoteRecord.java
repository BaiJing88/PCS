/*
 * Copyright (c) 2026 Bai_Jing88 (QQ: 1782307393)
 * PCS (Player Credit System) - Minecraft Cross-Server Player Management
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 * Any derivative work must also be open source and licensed under
 * the same AGPL v3 license. Commercial use is prohibited without
 * explicit permission from the author.
 */

package com.pcs.api.model;

import java.util.Date;
import java.util.UUID;

/**
 * 单个投票记录
 */
public class VoteRecord {
    
    private UUID voterUuid;
    private String voterName;
    private boolean agree;
    private Date voteTime;
    private String serverId;
    
    public VoteRecord() {
        this.voteTime = new Date();
    }
    
    public VoteRecord(UUID voterUuid, String voterName, boolean agree, String serverId) {
        this();
        this.voterUuid = voterUuid;
        this.voterName = voterName;
        this.agree = agree;
        this.serverId = serverId;
    }
    
    // Getters and Setters
    public UUID getVoterUuid() { return voterUuid; }
    public void setVoterUuid(UUID voterUuid) { this.voterUuid = voterUuid; }
    
    public String getVoterName() { return voterName; }
    public void setVoterName(String voterName) { this.voterName = voterName; }
    
    public boolean isAgree() { return agree; }
    public void setAgree(boolean agree) { this.agree = agree; }
    
    public Date getVoteTime() { return voteTime; }
    public void setVoteTime(Date voteTime) { this.voteTime = voteTime; }
    
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
}
