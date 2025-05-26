package com.example.coffeeshop.model;

public enum OrderStatus {
    NEW,          // Новый заказ, ожидает обработки
    PROCESSING,   // Заказ в обработке
    COMPLETED,    // Заказ выполнен
    CANCELLED     // Заказ отменен
} 