package com.worldcup.importreview.api.dto;

import java.util.List;

public record BatchApproveRequest(List<Long> itemIds) {
}
