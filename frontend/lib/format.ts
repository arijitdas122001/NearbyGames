const IST_TIME_ZONE = "Asia/Kolkata";

const dateFormatter = new Intl.DateTimeFormat("en-IN", {
  timeZone: IST_TIME_ZONE,
  weekday: "short",
  day: "numeric",
  month: "short",
  year: "numeric",
});

const timeFormatter = new Intl.DateTimeFormat("en-IN", {
  timeZone: IST_TIME_ZONE,
  hour: "numeric",
  minute: "2-digit",
  hour12: true,
});

const dateTimeFormatter = new Intl.DateTimeFormat("en-IN", {
  timeZone: IST_TIME_ZONE,
  weekday: "short",
  day: "numeric",
  month: "short",
  hour: "numeric",
  minute: "2-digit",
  hour12: true,
});

export function formatISTDate(value: string): string {
  return dateFormatter.format(new Date(value));
}

export function formatISTTime(value: string): string {
  return timeFormatter.format(new Date(value));
}

export function formatIST(value: string): string {
  return dateTimeFormatter.format(new Date(value));
}