import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ url }) => {
    const anchorUrlParam = url.searchParams.get('url');

    if (!anchorUrlParam) {
        // Return a specific error structure that the page can handle
        return { anchorMetadata: null, error: 'Missing anchor URL parameter' };
    }

    let decodedUrl: string;
    try {
        decodedUrl = decodeURIComponent(anchorUrlParam);
    } catch (e) {
        console.error('Error decoding anchor URL:', e);
        return { anchorMetadata: null, error: 'Invalid anchor URL encoding.' };
    }

    console.log(`Fetching anchor metadata from: ${decodedUrl}`);

    try {
        const response = await fetch(decodedUrl, {
            headers: {
                'Accept': 'application/ld+json, application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch anchor data (Status: ${response.status} ${response.statusText}) from ${decodedUrl}`);
        }

        const data = await response.json();

        // Structure the data for the page, including the raw JSON
        const anchorMetadata = {
            ...data, // Spread the fetched JSON data
            rawJson: JSON.stringify(data, null, 2) // Add raw JSON string
        };

        return {
            anchorMetadata: anchorMetadata,
            error: null
        };

    } catch (e) {
        console.error('Error fetching or parsing anchor data:', e);
        const errorMessage = e instanceof Error ? e.message : 'An unknown error occurred while processing the anchor URL.';
        // Ensure consistent return structure on error
        return {
            anchorMetadata: null,
            error: errorMessage
        };
    }
}; 