package com.example.demo.filter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Item {

    private Integer id;
    private ItemType type;
    private Integer age;
    // etc...
}
