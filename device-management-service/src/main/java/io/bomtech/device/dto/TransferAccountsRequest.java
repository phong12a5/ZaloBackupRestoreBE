package io.bomtech.device.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TransferAccountsRequest {
    private List<String> backedUpAccountIds;
    private String targetUserId;
}
