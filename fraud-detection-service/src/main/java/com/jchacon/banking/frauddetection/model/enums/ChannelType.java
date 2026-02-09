package com.jchacon.banking.frauddetection.model.enums;

import lombok.Getter;

@Getter
public enum ChannelType {
    WEB,
    MOBILE,
    ATM,
    POS;
}