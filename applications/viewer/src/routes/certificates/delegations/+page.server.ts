import type { PageServerLoad } from './$types'
import { env } from '$env/dynamic/public';

export const load: PageServerLoad = async ({params, url}) => {
    const page = url.searchParams.get('page') || '1';
    const count = url.searchParams.get('count') || '10';

    const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
    const apiUrl = `${INDEXER_BASE_URL}/stake/delegations?page=${page}&count=${count}`;
    console.log('Fetching delegations from:', apiUrl);

    try {
        const response = await fetch(apiUrl);
        console.log('Response status:', response.status);
        
        const data = await response.json();
        console.log('Raw API response:', JSON.stringify(data, null, 2));

        if (Array.isArray(data)) {
            console.log('First delegation entry:', data[0]);
            console.log('Total delegations:', data.length);
        }

        return {
            delegations: data,
            page: page,
            count: count
        };
    } catch (error) {
        console.error('Error fetching delegations:', error);
        return {
            delegations: [],
            page: page,
            count: count
        };
    }
};
