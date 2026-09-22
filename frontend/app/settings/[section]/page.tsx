import ProductArea from "../../../components/product-area";

export default async function SettingsPage({ params }: { params: Promise<{ section: string }> }) {
  const { section } = await params;
  return <ProductArea title={section.replaceAll("-", " ")} description="Make Wavelength feel like it was made for you." kind="settings" section={section} />;
}
