// Generic image-backed banner, reused by Travel's TripDetail and
// Property's CityDetail — same shape, only the image/title change. Title
// sits dead center (both axes) rather than pinned to an edge.
export function Cover({ image, title }: { image: string; title: string }) {
  return (
    <div
      className="flex min-h-[260px] items-center justify-center overflow-hidden rounded-2xl bg-cover bg-center bg-no-repeat bg-clip-padding px-6 py-10 text-center text-white"
      style={{ backgroundImage: `linear-gradient(180deg, rgba(20,63,64,.35) 0%, rgba(20,63,64,.55) 100%), url("${image}")` }}
    >
      <h1 className="font-display text-[clamp(1.8rem,4vw,2.6rem)] leading-[1.1] font-semibold">{title}</h1>
    </div>
  )
}
