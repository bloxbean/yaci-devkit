<script>

    import {Navbar, NavBrand, NavHamburger, NavLi, NavUl} from "flowbite-svelte";
</script>

<Navbar let:hidden let:toggle>
    <NavBrand href="/">
<!--        <img-->
<!--                src="https://flowbite.com/docs/images/logo.svg"-->
<!--                class="mr-3 h-6 sm:h-9"-->
<!--                alt="Flowbite Logo"-->
<!--        />-->
        <span class="self-center whitespace-nowrap text-xl font-semibold dark:text-white">
      Yaci Viewer
    </span>
    </NavBrand>
    <NavHamburger on:click={toggle} />
    <NavUl {hidden}>
        <NavLi href="/" active={true}>Home</NavLi>
        <NavLi href="/blocks">Blocks</NavLi>
        <NavLi href="/transactions">Transactions</NavLi>
    </NavUl>
</Navbar>
