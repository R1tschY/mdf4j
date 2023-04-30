module mdf4j {
  requires java.logging;
  requires static lombok;

  exports de.richardliebscher.mdf4;
  exports de.richardliebscher.mdf4.blocks;
  exports de.richardliebscher.mdf4.exceptions;
  exports de.richardliebscher.mdf4.io;
}