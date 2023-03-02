export function lovelaceToAda(num, decimalPlaces) {
    if (!decimalPlaces)
        return num / Math.pow(10, 6);
    else {
        let ada = num / Math.pow(10, 6);
        return ada.toFixed(decimalPlaces);
    }
}

