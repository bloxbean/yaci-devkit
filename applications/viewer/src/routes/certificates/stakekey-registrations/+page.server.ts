import type { PageLoad } from './$types'
import { env } from '$env/dynamic/public';

export const load: PageLoad = async ({params, url}) => {
    let page = url.searchParams.get('page') || "1";
    const count = 15;

    const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
    const apiUrl = `${INDEXER_BASE_URL}/stake/registrations?page=${page}&count=${count}`;
    console.log(apiUrl);

    const res = await fetch(apiUrl);
    const data = await res.json();

    const registrations  = data;
    console.log(data);

    if (res.ok) {
        return {
            registrations,
            total: 0,
            total_pages: 0,
            page: page,
            count: count
        }
    }

    return {
        status: 404,
        body: { error: 'Can not fetch stake registrations.' }
    };
}
