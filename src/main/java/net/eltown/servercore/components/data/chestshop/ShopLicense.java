package net.eltown.servercore.components.data.chestshop;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ShopLicense {

    private final String owner;
    private ShopLicenseType license;
    private int maxPossibleShops;

    public enum ShopLicenseType {

        STANDARD("Privatverkäufer"),
        SMALL_BUSINESS("Kleingewerbe"),
        BUSINESS("Gewerbe"),
        BIG_BUSINESS("Großgewerbe"),
        COMPANY("Unternehmen")

        ;

        private final String displayName;

        ShopLicenseType(final String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

}
