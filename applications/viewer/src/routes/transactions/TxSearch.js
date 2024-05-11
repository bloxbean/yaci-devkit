import {redirect} from "@sveltejs/kit";

/** @type {import('./$types').Actions} */
export const actions = {
    login: async ({ cookies, request, url }) => {
        const data = await request.formData();
        const tx_hash = data.get('txhash');
        const redirectUrl = `/transactions/${tx_hash}`;
        console.log(redirectUrl)

        throw redirect(301, redirectUrl);

    },
    register: async (event) => {
        // TODO register the user
    }
};
