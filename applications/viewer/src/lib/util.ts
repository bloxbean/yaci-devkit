export function formatAda(lovelace: number): string {
    const ada = lovelace / 1_000_000;
    return ada.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function formatLovelace(lovelace: number | undefined): string {
    if (lovelace === undefined) return '0 lovelace';
    return lovelace.toLocaleString() + ' lovelace';
}

// Function to truncate strings with a separator
export function truncate(fullStr: string | undefined | null, strLen: number, separator?: string): string {
    if (!fullStr) return ''; // Handle null or undefined input
    if (fullStr.length <= strLen) return fullStr;

    separator = separator || '...';

    var sepLen = separator.length,
        charsToShow = strLen - sepLen,
        frontChars = Math.ceil(charsToShow / 2),
        backChars = Math.floor(charsToShow / 2);

    return fullStr.substr(0, frontChars) +
           separator +
           fullStr.substr(fullStr.length - backChars);
}

// Function to format timestamp into a readable date/time string
export function getDate(timestamp: number | undefined | null): string {
    if (!timestamp) return ''; // Handle null or undefined input
    const date = new Date(timestamp * 1000);
    const options: Intl.DateTimeFormatOptions = {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
        hour12: false
    };
    return date.toLocaleString(undefined, options);
}

// Function to convert lovelace to Ada (potentially formatting)
export function lovelaceToAda(num: number | undefined | null, decimalPlaces?: number): string {
    if (num === undefined || num === null) return decimalPlaces ? Number(0).toFixed(decimalPlaces) : '0';

    const ada = num / 1_000_000;

    if (decimalPlaces !== undefined) {
        return ada.toFixed(decimalPlaces);
    }
    // Return full precision if decimalPlaces is not specified
    return ada.toString(); 
} 