package com.example.demo.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(access = AccessLevel.PRIVATE)
public class ResponseWrapper {

    private long projectId;
    private Item item;
    private ItemDetails itemDetails;

    public static ResponseWrapper of(Project project, Item item, ItemDetails itemDetails) {
        return ResponseWrapper.builder()
                .projectId(project.getId())
                .item(item)
                .itemDetails(itemDetails)
                .build();
    }
}
