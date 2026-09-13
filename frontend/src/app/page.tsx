import { redirect } from "next/navigation";
import { routing } from "@/i18n/routing";

/** Fallback if middleware does not rewrite `/` to the default locale. */
export default function RootPage() {
  redirect(`/${routing.defaultLocale}`);
}
