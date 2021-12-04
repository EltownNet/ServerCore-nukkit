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
    private int additionalShops;

    public enum ShopLicenseType {

        STANDARD("Privatverkäufer", 5, 0),
        SMALL_BUSINESS("Kleingewerbe", 40, 299.95),
        BUSINESS("Gewerbe", 80, 749.95),
        BIG_BUSINESS("Großgewerbe", 150, 2499.95),
        COMPANY("Unternehmen", 300, 7999.95)

        ;

        private final String displayName;
        private final int maxPossibleShops;
        private final double money;

        ShopLicenseType(final String displayName, final int maxPossibleShops, final double money) {
            this.displayName = displayName;
            this.maxPossibleShops = maxPossibleShops;
            this.money = money;
        }

        public String displayName() {
            return displayName;
        }

        public int maxPossibleShops() {
            return maxPossibleShops;
        }

        public double money() {
            return money;
        }
    }

}
