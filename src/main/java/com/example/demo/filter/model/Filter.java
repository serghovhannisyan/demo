package com.example.demo.filter.model;

import lombok.Data;

@Data
public class Filter {

    private final Integer value;
    private final ItemType type;

    public boolean matches(Item item) {
        return item.getAge() > value;
    }
}
